package com.redstoner.plots.storage.backing

import com.redstoner.plots.*
import com.redstoner.plots.storage.SerializablePlot
import com.redstoner.plots.util.loop
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.sql.Connection
import java.util.*


class MySqlBacking(val driver: SQLDriver) : Backing {
    override val name get() = driver.name
    private inline val String.prefixed get() = this.replace("{prefix}", "RedstonerPlots-")

    override fun init() {
        try {
            driver.init()

            if (!tableExists("{prefix}plots".prefixed)) {
                val schemaFile = "schema/${name.toLowerCase()}.sql"
                (Main.instance.getResource(schemaFile) ?: throw Error("Didn't find schema file for $name")).use {
                    BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use buf@ {
                        driver.connection.use {
                            createStatement().use {
                                val builder = StringBuilder()
                                var line: String?
                                loop {
                                    line = this@buf.readLine()
                                    if (line == null) doBreak()
                                    if (line!!.startsWith("--") || line!!.startsWith(("#"))) doContinue()
                                    builder.append(line)
                                    if (line!!.endsWith(";")) {
                                        builder.deleteCharAt(builder.length - 1)
                                        val statement = builder.toString().trim().prefixed
                                        if (!statement.isBlank()) {
                                            addBatch(statement)
                                        }
                                    }
                                }
                                executeBatch()
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Main.instance.logger.severe("Failed to initialise the database")
            close()
        }
    }

    override fun close() {
        try {
            driver.shutdown()
        } catch (ex: Exception) {

        }
    }

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

    override val plotDataProducer: suspend ProducerScope<Pair<Plot, PlotData>>.(Sequence<Plot>) -> Unit
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    suspend override fun readPlotData(plotFor: Plot): PlotData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun getOwnedPlots(user: PlotOwner): Sequence<SerializablePlot> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotData(plotFor: Plot, data: PlotData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotOwner(plotFor: Plot, owner: PlotOwner) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotOptions(plotFor: Plot, options: PlotOptions) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

abstract class SQLDriver {

    abstract val name: String

    abstract fun init()

    abstract fun shutdown()

    abstract val connection: Connection?

}

class MySqlDriver(override val name: String, val o: DataStorageOptions, val driverClass: String) : SQLDriver() {
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
