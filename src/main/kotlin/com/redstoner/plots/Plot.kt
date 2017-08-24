package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.util.equalsNullable
import com.redstoner.plots.util.getPlayerName
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

// convenience functions when the type is nullable
fun PlotData?.canBuild(user: Player): Boolean = this?.canBuild(user) == true
fun PlotData?.isAllowed(user: Player): Boolean = this?.isAllowed(user) == true
fun PlotData?.isBanned(user: Player): Boolean = this?.isBanned(user) == true
val PlotData?.allowsInteractInputs: Boolean get() = this?.allowsInteractInputs ?: false

internal inline val st get() = Main.instance.storage

class Plot(val world: PlotWorld,
           val coord: Vec2i,
           var data: PlotData = BasePlotData()) : PlotData by data {

    val id get() = "${coord.x}:${coord.z}"

    override fun toString(): String = "Plot(world=$world, coord=$coord)"

    fun canBuild(player: Player): Boolean = isAllowed(player) || (owner?.matches(player) ?: false)

    fun hasBlockVisitors(): Boolean = false

    /*
    Data delegation
     */
    override fun setPlayerState(uuid: UUID, state: Boolean?): Boolean {
        if (data.setPlayerState(uuid, state)) {
            st.setPlotPlayerState(this, uuid, state)
            return true
        }
        return false
    }

    override var allowsInteractInventory: Boolean
        get() = data.allowsInteractInventory
        set(value) {
            if (allowsInteractInventory != value) {
                st.setPlotAllowsInteractInventory(this, value)
                data.allowsInteractInventory = value
            }
        }

    override var allowsInteractInputs: Boolean
        get() = data.allowsInteractInputs
        set(value) {
            if (allowsInteractInputs != value) {
                st.setPlotAllowsInteractInputs(this, value)
                data.allowsInteractInputs = value
            }
        }

    override var owner: PlotOwner?
        get() = data.owner
        set(value) {
            if (!value.equalsNullable(data.owner)) {
                st.setPlotOwner(this, value)
                data.owner = value
            }
        }

}

interface PlotData {
    var isLoaded: Boolean
    var owner: PlotOwner?
    val bannedPlayers: Collection<UUID>
    val allowedPlayers: Collection<UUID>
    var allowsInteractInputs: Boolean
    var allowsInteractInventory: Boolean

    fun dataIsEmpty(): Boolean

    fun getPlayerState(uuid: UUID): Boolean?

    // returns true if a change was made
    fun setPlayerState(uuid: UUID, state: Boolean?): Boolean

    fun isAllowed(player: Player) = getPlayerState(player.uniqueId) == true

    fun isBanned(player: Player) = getPlayerState(player.uniqueId) == false

    fun ban(player: Player): Boolean = setPlayerState(player.uniqueId, false)

    fun allow(player: Player): Boolean = setPlayerState(player.uniqueId, true)

    fun neutralize(player: Player): Boolean = setPlayerState(player.uniqueId, null)
}

class BasePlotData : PlotData {
    private val addedPlayers = HashMap<UUID, Boolean>()
    //@formatter:off
    override @Volatile var isLoaded = false
    override @Volatile var owner: PlotOwner? = null
    override @Volatile var allowsInteractInputs = true
    override @Volatile var allowsInteractInventory = true
    //@formatter:on

    override val allowedPlayers @Synchronized get() = addedPlayers.filterValues { it }.keys

    override val bannedPlayers @Synchronized get() = addedPlayers.filterValues { !it }.keys

    override fun getPlayerState(uuid: UUID) = addedPlayers[uuid]

    @Synchronized
    override fun setPlayerState(uuid: UUID, state: Boolean?): Boolean {
        if (state == null) {
            return addedPlayers.remove(uuid) != null
        } else {
            return addedPlayers.put(uuid, state) != state
        }
    }

    override fun dataIsEmpty(): Boolean {
        return allowsInteractInventory
                && allowsInteractInputs
                && owner === null
                && addedPlayers.isEmpty()
    }
}

data class PlotOwner(val uuid: UUID? = null,
                     val name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val playerName: String
        get() {
            return getPlayerName(uuid ?: return name!!, name ?: ":unknownName:")
        }

    val offlinePlayer get() = if (uuid != null) Bukkit.getOfflinePlayer(uuid) else Bukkit.getOfflinePlayer(name)

    fun matches(player: Player, allowNameMatch: Boolean = false): Boolean {
        return player.uniqueId == uuid || (allowNameMatch && player.name == name)
    }

}
