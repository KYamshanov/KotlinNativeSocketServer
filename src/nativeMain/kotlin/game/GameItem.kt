package game

enum class GameItem(name: String) {

    STONE("Камень"),
    SCISSORS("Ножницы"),
    PAPER("Бумага");

    fun fight(item: GameItem): GameItem? =
        when (this) {
            STONE -> when (item) {
                STONE -> null
                PAPER -> item
                SCISSORS -> this
            }
            SCISSORS -> when (item) {
                SCISSORS -> null
                STONE -> item
                PAPER -> this
            }
            PAPER -> when (item) {
                PAPER -> null
                SCISSORS -> item
                STONE -> this
            }
        }
}