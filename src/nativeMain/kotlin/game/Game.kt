package game

import ServerSocket

interface Game {
    val gameStatus: GameStatus

    fun joinPlayer(player: Player)
    fun removePlayer(player: Player)
    fun selectItem(clientId: Int, gameItem: GameItem)
    fun getStatus(): String

    enum class GameStatus(val status: String) {
        WAIT("Ожидание"),
        GAME("Игра")
    }
}

fun produceGame(
    serverSocket: ServerSocket,
    needPlayers: Int
): Game =
    GameImpl(serverSocket, needPlayers)


private class GameImpl(
    private val serverSocket: ServerSocket,
    private val needPlayers: Int
) : Game {

    private val players = mutableListOf<Player>()
    private val pairs = mutableListOf<Pair<Player, Player>>()
    private var step = 0

    override var gameStatus: Game.GameStatus = Game.GameStatus.WAIT
        private set


    override fun joinPlayer(player: Player) {
        if (gameStatus == Game.GameStatus.WAIT) {
            if (!players.contains(player)) {
                players.add(player)
                broadcastMessage("Игрок ${player.playerName} присоединился")
            }
        }
        if (players.size >= needPlayers) {
            startGame()
        }
    }

    override fun removePlayer(player: Player) {
        players.removeAll { it.clientId == player.clientId }
        player.disconnect()
        broadcastMessage("Игрок ${player.playerName} покинул игру")
    }

    override fun selectItem(clientId: Int, gameItem: GameItem) {
        players.firstOrNull { it.clientId == clientId }?.apply {
            selectedItem = gameItem
        }?.also {
            broadcastMessage("Игрок ${it.playerName} сделал выбор")
        }
        val waitingPlayers = getWaitingPlayers()
        if (waitingPlayers.isEmpty()) {
            fightBetweenPlayers()
        } else {
            broadcastMessage("Ожидаем игрока: ${waitingPlayers.joinToString(", ") { it.playerName }}")
        }
    }

    override fun getStatus(): String = "${gameStatus.status} Игроков: ${players.size}/${needPlayers}"

    private fun broadcastMessage(message: String) {
        players.forEach { it.sendMessage(message) }
    }

    private fun startGame() {

        gameStatus = Game.GameStatus.GAME
        broadcastMessage("Игра началась")
        players.forEach { it.selectedItem = null }
        broadcastMessage("Введите Камень, Ножницы или Бамага")
        nextStep()
    }

    private fun nextStep() {
        step++
        broadcastMessage("Раунд: $step")
        matchWithPairs()
        pairs.forEach {
            it.first.sendMessage("Вы играете с ${it.second.playerName}")
            it.second.sendMessage("Вы играете с ${it.first.playerName}")
        }
        if (players.size % 2 != 0)
            players[0].sendMessage("Вам не нашлась пара, ожидайте")
    }

    private fun getWaitingPlayers(): List<Player> =
        pairs.flatMap { listOf(it.first, it.second) }.filter { it.selectedItem == null }

    private fun matchWithPairs() {
        for (i in players.indices step 2)
            pairs.add(players[i] to players[i + 1])
    }

    private fun fightBetweenPlayers() {
        pairs.forEach {
            it.first.fightWith(it.second)?.let { winner ->
                if (winner == it.first) {
                    winPlayer(it.first)
                    losePlayer(it.second)
                } else {
                    winPlayer(it.second)
                    losePlayer(it.first)
                }
            } ?: run {
                it.first.sendMessage("Ничья")
                it.second.sendMessage("Ничья")
            }
        }
        if (players.size == 1)
            gameStop(players[0])
    }

    private fun gameStop(winner: Player) {
        broadcastMessage("Победил игрок: ${winner}")
        gameStatus = Game.GameStatus.WAIT
        players.clear()
        pairs.clear()
        step = 0
    }

    private fun winPlayer(player: Player) {
        player.sendMessage("Вы победили в схватке")
    }

    private fun losePlayer(player: Player) {
        players.removeAll { it.clientId == player.clientId }
        player.sendMessage("Вы проиграли(")
    }

    private fun wipeData() {
        players.clear()
        pairs.clear()
        step = 0
    }

}