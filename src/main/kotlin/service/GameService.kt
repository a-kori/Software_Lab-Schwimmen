package service
import exceptions.*
import entity.*
import view.Refreshable

/**
 * A service class, used to describe the
 * logic of the game's flow control.
 */
class GameService (private val rs : RootService) : AbstractRefreshingService() {

    /**
     * Initializes a new game, adds a list of
     * players with passed [playerNames] and
     * randomly distributes cards between the
     * players and the card stack on the table.
     */
    fun startGame (playerNames : List<String>) {
        initializePlayers(playerNames)
        distributeCards()

        onAllRefreshables(Refreshable::refreshAfterStartNewGame)
    }

    /**
     * Initializes a player list with passed
     * [playerNames], if [playerNames] is valid,
     * and initializes a new game with the list.
     */
    private fun initializePlayers (playerNames : List<String>) {
        if (playerNames.size < 2)
            throw PlayerCountException("The minimal number of players (2) is not reached!")

        if (playerNames.size > 4)
            throw PlayerCountException("The maximal number of players (4) is exceeded!")

        for (name in playerNames) {
            if (name.isBlank())
                throw PlayerNameException("One or more of the assigned player names are blank!")
        }
        if (playerNames != playerNames.distinct())
            throw PlayerNameException("One or more of the assigned player names are duplicated!")

        rs.currentGame = Game( Array(playerNames.size) { i -> Player(playerNames[i]) } )
    }

    /**
     * Initializes the unusedCards stack and distributes
     * cards from it to the players and the open card stack
     */
    private fun distributeCards() {
        val game = rs.currentGame

        for ( suit in CardSuit.getAllSuits() ) {
            for ( value in CardValue.getAllValuesReduced() ) {
                game.unusedCards.add( Card(suit, value) )
            }
        }
        game.unusedCards.shuffle()

        if (game.unusedCards.size < game.openCards.size + game.players.size * game.players[0].cards.size)
            throw NoCardsLeftException("There are not enough cards in the draw pile to start the game!")

        for (player in game.players) {
            for(i in 0 until player.cards.size) {
                player.cards[i] = game.unusedCards[0]
                rs.playerService.updateScore(player)
                game.unusedCards.removeAt(0)
            }
        }
        renewOpenCards()
    }

    /**
     * Checks if there are enough cards
     * left in the draw stack to renew
     * the open card stack.
     */
    fun enoughCardsLeft() : Boolean {
        val game = rs.currentGame
        return game.unusedCards.size >= game.openCards.size
    }

    /**
     * Refreshes the open card stack with cards from
     * the draw pile, if there are enough cards.
     */
    fun renewOpenCards() {
        val game = rs.currentGame
        if (!enoughCardsLeft())
            throw NoCardsLeftException("There are not " +
                    "enough cards in the draw pile to renew the card stack on the table!")

        for(i in 0 until game.openCards.size) {
            game.openCards[i] = game.unusedCards[0]
            game.unusedCards.removeAt(0)
        }
    }

    /**
     * Sets playerIndex to the index of the next player,
     * whose turn it is to choose a game action. But, if
     * this player has knocked in the previous round of
     * the game, the game is finished.
     */
    fun nextPlayer() {
        val game = rs.currentGame

        rs.playerIndex += 1
        if (rs.playerIndex == game.players.size)
            rs.playerIndex = 0

        if (game.players[rs.playerIndex].hasKnocked) {
            endGame()
        }
        else onAllRefreshables(Refreshable::refreshAfterGameTurn)
    }

    /**
     * Ends the game by breaking the game loop and sorting players
     * in the descending order as to the scores they achieved in the game.
     */
    fun endGame() {
        rs.currentGame.players.sortByDescending { player -> player.score }
        onAllRefreshables(Refreshable::refreshAfterGameOver)
    }

    /**
     * Checks if the current [GameService] object is equal to the passed
     * [other] object.
     */
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (javaClass != other.javaClass) return false

        other as GameService

        if (rs !== other.rs) return false
        if (refreshables != other.refreshables) return false

        return true
    }

    /**
     * Returns the hash code of the current [GameService] object.
     * Auto-generated by IntelliJ.
     */
    override fun hashCode(): Int {
        return 31 * rs.hashCode() + refreshables.hashCode()
    }

}