package com.redstoner.plots.storage.backing

import com.redstoner.plots.*
import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.storage.SerializablePlot
import com.redstoner.plots.storage.SerializableWorld
import com.redstoner.plots.util.toByteArray
import com.redstoner.plots.util.toUUID
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.io.InputStream
import java.sql.Connection
import java.sql.Statement
import java.util.*

class SqlBacking(val driver: SqlDriver) : Backing {
    private companion object {
        @JvmStatic
        val plotQuery = "select plot_id, owner_name, owner_uuid, opt_interact_inventory, " +
                "opt_interact_inputs from plots where world_id = ? and idx = ? and idz = ?;"

        @JvmStatic
        val localAddedQuery = "SELECT uuid, flag FROM added_local WHERE plot_id = ?;"
    }

    override val name get() = driver.name

    private fun printErr(err: String) = Main.instance.logger.severe("[Storage error $name] $err")

    private inline fun <R> conn(block: Connection.() -> R): R? {
        try {
            return driver.connection.use(block)
        } catch (ex: Exception) {
            ex.printStackTrace()
            printErr("Failed to connect to database")
            return null
        }
    }

    private inline fun <R> connCatch(msg: () -> String, block: Connection.() -> R): R? = conn {
        try {
            block(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
            printErr(msg())
            null
        }
    }

    override suspend fun init() {
        try {
            driver.init()

            if (!tableExists("plots")) {
                val schemaFile = "schema/${name.toLowerCase()}.sql"
                val stream = (Main.instance.getResource(schemaFile) ?: throw Exception("Didn't find schema file for $name"))
                driver.connection.use {
                    createStatement().use { executeBatch(stream, this) }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            printErr("Failed to initialise the database")
            shutdown()
        }
    }

    private fun getWorldId(conn: Connection, uid: UUID, name: String): Int {
        return conn.prepareStatement("SELECT world_id FROM worlds WHERE uid = unhex(?);").use {
            val uidBytes = uid.toByteArray()
            setBytes(1, uidBytes)
            executeQuery().use {
                if (next()) {
                    getInt(1)
                } else {
                    conn.prepareStatement("INSERT IGNORE worlds VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS).use {
                        setString(1, name)
                        setBytes(2, uidBytes)
                        executeUpdate()
                        generatedKeys.use {
                            next()
                            getInt(1)
                        }
                    }
                }
            }
        }
    }

    private fun getPlotId(conn: Connection, worldUID: UUID, worldName: String, idx: Int, idz: Int): Int {
        return conn.prepareStatement("SELECT plot_id FROM plots WHERE world_id = ? and idx = ? and idz = ?;").use {
            val worldId = getWorldId(conn, worldUID, worldName)
            setInt(1, worldId)
            setInt(2, idx)
            setInt(3, idz)
            executeQuery().use {
                if (next()) {
                    getInt(1)
                } else {
                    conn.prepareStatement("INSERT IGNORE plots VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS).use {
                        setInt(1, worldId)
                        setInt(2, idx)
                        setInt(3, idz)
                        executeUpdate()
                        generatedKeys.use {
                            next()
                            getInt(1)
                        }
                    }
                }
            }
        }
    }

    private fun executeBatch(stream: InputStream, statement: Statement) {
        val lines = stream.bufferedReader().use { readLines() }
        var sb = StringBuilder()
        lines.filter { !it.startsWith("#") }.forEach {
            sb.append(it)
            if (it.endsWith(";")) {
                sb.deleteCharAt(sb.length - 1)
                val stString = sb.toString().trim()
                statement.addBatch(stString)
                sb = StringBuilder()
            }
        }
        statement.executeBatch()
    }

    override suspend fun shutdown() {
        try {
            driver.shutdown()
        } catch (ex: Exception) {
            ex.printStackTrace()
            printErr("Failed to shutdown the database")
        }
    }

    @Throws(Exception::class)
    private fun tableExists(tableName: String): Boolean {
        driver.connection.use {
            metaData.getTables(null, null, "%", null).use {
                while (next()) {
                    if (getString(3).equals(tableName, ignoreCase = true)) {
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun readPlotData(conn: Connection, plotFor: Plot): PlotData? {
        try {
            conn.prepareStatement(plotQuery).use {
                val world = plotFor.world.world
                setInt(1, getWorldId(conn, world.uid, world.name))
                setInt(2, plotFor.coord.x)
                setInt(3, plotFor.coord.z)
                executeQuery().use {
                    if (!next()) {
                        return null
                    }

                    val plotId = getLong(1)
                    val ownerName = getString(2)
                    val ownerUUID = getBytes(3).toUUID()
                    val optInteractInputs = getBoolean(4)
                    val optInteractInventory = getBoolean(5)

                    val data = BasePlotData()
                    if (ownerName != null || ownerUUID != null) {
                        data.owner = PlotOwner(ownerUUID, ownerName)
                    }
                    data.allowsInteractInventory = optInteractInventory
                    data.allowsInteractInputs = optInteractInputs

                    conn.prepareStatement(localAddedQuery).use {
                        setLong(1, plotId)
                        executeQuery().use {
                            while (next()) {
                                val uuid = getBytes(1).toUUID() ?: continue
                                val flag = getBoolean(2)
                                data.setPlayerState(uuid, flag)
                            }
                        }
                    }

                    return data
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            printErr("Failed to read plot data for plot $plotFor")
            return null
        }
    }

    override val producePlotData: suspend ProducerScope<Pair<Plot, PlotData?>>.(Sequence<Plot>) -> Unit
        get() = { conn { it.forEach { send(Pair(it, readPlotData(this, it))) } } }

    suspend override fun readPlotData(plotFor: Plot): PlotData? = conn { readPlotData(this, plotFor) }

    suspend override fun getOwnedPlots(user: PlotOwner): List<SerializablePlot> {
        // so much indentation? Idk
        val uuid = user.uuid ?: return emptyList()
        return connCatch({ "Failed to get all plots owned by player with uuid ${user.uuid}" }) {
            val list = ArrayList<SerializablePlot>()
            prepareStatement("SELECT world_id, idx, idz FROM plots WHERE owner_uuid = ?;").use {
                setBytes(1, uuid.toByteArray())
                executeQuery().use rs1@ {
                    while (next()) {
                        val worldId = getInt(1)
                        prepareStatement("SELECT name, uid FROM worlds WHERE world_id = ?;").use {
                            setInt(1, worldId)
                            executeQuery().use {
                                if (next()) {
                                    val plot = SerializablePlot(
                                            coord = Vec2i(this@rs1.getInt(2), this@rs1.getInt(3)),
                                            world = SerializableWorld(
                                                    name = getString(1),
                                                    uid = getBytes(2).toUUID()))
                                    list.add(plot)
                                }
                            }
                        }
                    }
                }
            }
            list
        } ?: emptyList()
    }
/*
    suspend override fun setPlotData(plotFor: Plot, data: PlotData) {
        connCatch({ "Failed to set plot data for $plotFor" }) {
            val world = plotFor.world.world
            val plotId = getPlotId(this, world.uid, world.name, plotFor.coord.x, plotFor.coord.z)
            val reset = data.dataIsEmpty()

            if (reset) {
                prepareStatement("DELETE FROM plots WHERE plot_id = ?;").use {
                    setInt(1, plotId)
                    executeUpdate()
                }
            } else {
                prepareStatement("UPDATE plots SET owner_uuid = ?, owner_name = ?, opt_outsider_i_inventory = ?, opt_outsider_i_inputs = ?" +
                        " WHERE plot_id = ?;").use {
                    setBytes(1, data.owner?.uuid.toByteArray())
                    setString(2, data.owner?.name)
                    setBoolean(3, data.allowsInteractInventory)
                    setBoolean(4, data.allowsInteractInputs)
                    setInt(5, plotId)
                    executeUpdate()
                }
            }

            prepareStatement("DELETE FROM added_local WHERE plot_id = ?;").use {
                setInt(1, plotId)
                executeUpdate()
            }

            if (!reset) {
                prepareStatement("INSERT INTO added_local VALUES (?, ?, ?);").use {
                    data.addedPlayers.forEach { uuid, flag ->
                        setInt(1, plotId)
                        setBytes(2, uuid.toByteArray())
                        setBoolean(3, flag)
                        addBatch()
                    }
                    executeBatch()
                }
            }

        }
    }*/

    suspend override fun setPlotOwner(plotFor: Plot, owner: PlotOwner?) {
        connCatch({ "Failed to set plot owner for $plotFor" }) {
            val world = plotFor.world.world
            val plotId = getPlotId(this, world.uid, world.name, plotFor.coord.x, plotFor.coord.z)

            prepareStatement("UPDATE plots SET owner_uuid = ?, owner_name = ? WHERE plot_id = ?;").use {
                setBytes(1, owner?.uuid.toByteArray())
                setString(2, owner?.name)
                setInt(3, plotId)
                executeUpdate()
            }
        }
    }

    suspend override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) {
        connCatch({ "Failed to set plot player state for $plotFor and player with uuid $player" }) {
            val world = plotFor.world.world
            val plotId = getPlotId(this, world.uid, world.name, plotFor.coord.x, plotFor.coord.z)

            if (state != null) {
                prepareStatement("REPLACE added_local VALUES (?, ?, ?);").use {
                    setInt(1, plotId)
                    setBytes(2, player.toByteArray())
                    setBoolean(3, state)
                    executeUpdate()
                }
            } else {
                prepareStatement("DELETE FROM added_local WHERE plot_id = ? AND uuid = ?;").use {
                    setInt(1, plotId)
                    setBytes(2, player.toByteArray())
                    executeUpdate()
                }
            }
        }
    }

    suspend override fun setPlotAllowsInteractInventory(plotFor: Plot, value: Boolean) {
        connCatch({ "Failed to set plot option allowInteractInventory for $plotFor" }) {
            val world = plotFor.world.world
            val plotId = getPlotId(this, world.uid, world.name, plotFor.coord.x, plotFor.coord.z)

            prepareStatement("UPDATE plots SET opt_interact_inventory = ? WHERE plot_id = ?;").use {
                setBoolean(1, value)
                setInt(2, plotId)
                executeUpdate()
            }
        }
    }

    suspend override fun setPlotAllowsInteractInputs(plotFor: Plot, value: Boolean) {
        connCatch({ "Failed to set plot option allowInteractInputs for $plotFor" }) {
            val world = plotFor.world.world
            val plotId = getPlotId(this, world.uid, world.name, plotFor.coord.x, plotFor.coord.z)

            prepareStatement("UPDATE plots SET opt_interact_inputs = ? WHERE plot_id = ?;").use {
                setBoolean(1, value)
                setInt(2, plotId)
                executeUpdate()
            }
        }
    }
}

abstract class SqlDriver {

    abstract val name: String

    abstract fun init()

    abstract fun shutdown()

    abstract val connection: Connection?

}

class MySqlDriver(override val name: String, val o: DataConnectionOptions, val driverClass: String) : SqlDriver() {
    private var source: HikariDataSource? = null

    override fun init() {
        val (address, port) = o.splitAddressAndPort(3306) ?: throw IllegalArgumentException("Invalid data storage options")

        source = with(HikariConfig()) {
            poolName = "redstonerplots"
            maximumPoolSize = o.poolSize
            dataSourceClassName = driverClass
            username = o.username
            password = o.password
            connectionTimeout = 15000
            leakDetectionThreshold = 10000
            connectionTestQuery = "SELECT 1"

            addDataSourceProperty("serverName", address)
            addDataSourceProperty("port", port.toString())
            addDataSourceProperty("databaseName", o.database)

            // copied from github.com/lucko/LuckPerms
            if (name.toLowerCase() == "mariadb") {
                addDataSourceProperty("properties", "useUnicode=true;characterEncoding=utf8")
            } else {
                // doesn't exist on the MariaDB driver
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("alwaysSendSetIsolation", "false")
                addDataSourceProperty("cacheServerConfiguration", "true")
                addDataSourceProperty("elideSetAutoCommits", "true")
                addDataSourceProperty("useLocalSessionState", "true")

                // already set as default on mariadb
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("cacheCallableStmts", "true")

                // make sure unicode characters can be used.
                addDataSourceProperty("characterEncoding", "utf8")
                addDataSourceProperty("useUnicode", "true")
            }

            HikariDataSource(this)
        }
    }

    override fun shutdown() = source?.close() ?: Unit

    override val connection get() = source?.connection

}
