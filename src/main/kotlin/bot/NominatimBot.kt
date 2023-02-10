package bot

import algo.PubsHandler
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.logging.LogLevel
import data.remote.repository.NominatimRepository
import kotlinx.coroutines.*
import texts.BotTexts

private const val BOT_TOKEN = "5883735351:AAGFBLb8VbACrTWJQUTD209RQZCvR5uWvd8"
private const val TIMEOUT_TIME = 30

class NominatimBot(private val nominatimRepository: NominatimRepository, private val pubsHandler: PubsHandler) {
    fun createBot(): Bot {
        return bot {
            token = BOT_TOKEN
            timeout = TIMEOUT_TIME
            logLevel = LogLevel.Network.Body

            dispatch {
                setUpCommands()
                setUpCallbacks()
                setUpMessages()
            }
        }
    }

    data class ClientInfo(
        var status: Mode,
        var firstPlace: String,
        var secondPlace: String,
        var pubs: ArrayList<PubsHandler.Vertex>,
        var pubsCount: Int
    )

    private val clients = mutableMapOf<Long, ClientInfo>()


    private fun Dispatcher.setUpCallbacks() {
        callbackQuery(callbackData = "two_different") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery

            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "${BotTexts.ENTER_FIRST_LOCATION}\n${BotTexts.ENTER_ATTENTION}"
            )
            clients[chatId]!!.status = Mode.READ_FIRST_LOCATION;
        }

        callbackQuery(callbackData = "from_location") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = BotTexts.GIVE_ME_LOCATION
            )
            clients[chatId]!!.status = Mode.GET_FIRST_LOCATION;
        }

        callbackQuery(callbackData = "ok_first_location") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            clients[chatId]!!.status = Mode.READ_SECOND_LOCATION
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "${BotTexts.ENTER_SECOND_LOCATION}\n${BotTexts.ENTER_ATTENTION}"
            )
        }

        callbackQuery(callbackData = "show_all_pubs") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)
            val text: StringBuilder = java.lang.StringBuilder()
            for (i in 0 until clients[chatId]!!.pubs.size) {
                if (i % 10 == 0) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = text.toString()
                    )
                    text.clear()
                }
                text.append("${i+1}. ${clients[chatId]!!.pubs[i].name}\n")
            }
            if (text.isNotEmpty()) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = text.toString()
                )
            }
            clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            bot.sendMessage(chatId = ChatId.fromId(chatId), text = BotTexts.TO_MAKE_NEW_REQUEST)
        }

        callbackQuery(callbackData = "ok_second_location") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            clients[chatId]!!.status = Mode.READ_PUBS_COUNT
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = BotTexts.SHOW_K_PUBS
            )
        }

        callbackQuery(callbackData = "ok_count") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            clients[chatId]!!.status = Mode.IN_PROCESS
            bot.apply {
                sendMessage(chatId = ChatId.fromId(chatId), text = BotTexts.FIND_PROCESS)
                sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val firstCoordinates = nominatimRepository.getCoordiantesByName(
                    clients[chatId]!!.firstPlace,
                    "json",
                    "1"
                )[0]
                val secondCoordinates = nominatimRepository.getCoordiantesByName(
                    clients[chatId]!!.secondPlace,
                    "json",
                    "1"
                )[0]

                val newFirstLon = firstCoordinates.lon.toDouble() + 0.001
                val newFirstLat = firstCoordinates.lat.toDouble() + 0.001
                val newSecondLon = secondCoordinates.lon.toDouble() - 0.001
                val newSecondLat = secondCoordinates.lat.toDouble() - 0.001

                val places = nominatimRepository.getObjectsBetween(
                    "pub",
                    "json",
                    "1",
                    "${newFirstLon},${newFirstLat},${newSecondLon},${newSecondLat}",
                    "${minOf(50, clients[chatId]!!.pubsCount)}"
                )
                if (places.isNotEmpty()) {
                    clients[chatId]!!.pubs = pubsHandler.findPath(places, firstCoordinates)
                    clients[chatId]!!.status = Mode.FOUND_PUBS
                    val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(
                                text = BotTexts.SHOW_ALL_PUBS,
                                callbackData = "show_all_pubs"
                            )
                        )
                    )
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = BotTexts.FIND_PUBS + clients[chatId]!!.pubs.size + " " + BotTexts.PUBS,
                        replyMarkup = inlineKeyboardMarkup
                    )
                } else {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = BotTexts.NONE_PUBS
                    )
                    clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
                    bot.sendMessage(chatId = ChatId.fromId(chatId), text = BotTexts.TO_MAKE_NEW_REQUEST)
                }
            }
        }
    }

    private fun Dispatcher.setUpMessages() {
        location {
            fun getFirstLocation(chatId: Long) {
                CoroutineScope(Dispatchers.IO).launch {
                    val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(
                                text = BotTexts.YES_BUTTON,
                                callbackData = "ok_first_location"
                            )
                        )
                    )
                    clients[chatId]!!.firstPlace = nominatimRepository.getCountryNameByCoordinates(
                        latitude = location.latitude.toString(),
                        longitude = location.longitude.toString(),
                        format = "json"
                    ).display_name
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = BotTexts.AM_I_RIGHT +
                                ":\n" + clients[chatId]!!.firstPlace + "\n\n" +
                                BotTexts.QUESTION + " " + BotTexts.ENTER_AGAIN,
                        replyMarkup = inlineKeyboardMarkup
                    )
                }

            }

            fun dontNeedLocation(chatId: Long) {
                bot.sendMessage(chatId = ChatId.fromId(chatId), text = BotTexts.DONT_KNOW)
            }

            val chatId = message.chat.id
            when (clients[message.chat.id]!!.status) {
                Mode.GET_FIRST_LOCATION -> getFirstLocation(chatId)
                else -> dontNeedLocation(chatId)
            }
        }

        message(Filter.Text) {
            fun unknownSituation(chatId: Long) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = BotTexts.DONT_KNOW
                )
            }

            fun iAmBusy(chatId: Long) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = BotTexts.IN_PROCESS
                )
            }

            fun readLocation(chatId: Long, location: Int) {
                CoroutineScope(Dispatchers.IO).launch {
                    val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(
                                text = BotTexts.YES_BUTTON,
                                callbackData = if (location == 1) "ok_first_location" else "ok_second_location"
                            )
                        )
                    )
                    val response = nominatimRepository.getCoordiantesByName(
                        message.text.toString(),
                        "json",
                        "1"
                    )
                    if (response.size == 0) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = BotTexts.FIND_NOTHING
                        )
                    } else {
                        if (location == 1) {
                            clients[chatId]!!.firstPlace = response[0].display_name
                        } else {
                            clients[chatId]!!.secondPlace = response[0].display_name
                        }
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = BotTexts.AM_I_RIGHT +
                                    ":\n" + response[0].display_name + "\n\n" +
                                    BotTexts.QUESTION + " " + BotTexts.ENTER_AGAIN,
                            replyMarkup = inlineKeyboardMarkup
                        )
                    }
                }
            }

            fun waitingForLocation(chatId: Long) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = BotTexts.WAIT_LOCATION
                )
            }

            fun waitingForNumber(chatId: Long) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = BotTexts.NOT_NUMBER
                )
            }

            fun readPubsCount(chatId: Long) {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = BotTexts.YES_BUTTON,
                            callbackData = "ok_count"
                        )
                    )
                )
                val count = message.text.toString()
                try {
                    clients[chatId]!!.pubsCount = count.toInt()
                } catch (error: NumberFormatException) {
                    waitingForNumber(chatId)
                    return
                }
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = BotTexts.AM_I_RIGHT +
                            ":\n" + count + "\n\n" +
                            BotTexts.QUESTION + " " + BotTexts.ENTER_AGAIN,
                    replyMarkup = inlineKeyboardMarkup
                )
            }

            val chatId = message.chat.id
            if (!clients.containsKey(chatId)) {
                clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            }
            when (clients[message.chat.id]!!.status) {
                Mode.START -> unknownSituation(chatId)
                Mode.READ_FIRST_LOCATION -> readLocation(chatId, 1)
                Mode.READ_SECOND_LOCATION -> readLocation(chatId, 2)
                Mode.GET_FIRST_LOCATION -> waitingForLocation(chatId)
                Mode.IN_PROCESS -> iAmBusy(chatId)
                Mode.READ_PUBS_COUNT -> readPubsCount(chatId)
                else -> unknownSituation(chatId)
            }
        }
    }

    private fun Dispatcher.setUpCommands() {
        command("start") {
            val keyboardMarkup = KeyboardReplyMarkup(
                keyboard = listOf(
                    listOf(KeyboardButton("/start")),
                    listOf(KeyboardButton("/help")),
                    listOf(KeyboardButton("/find")),
                    listOf(KeyboardButton("/restart"))
                ),
                resizeKeyboard = true
            )
            val chatId = message.chat.id
            if (!clients.containsKey(chatId)) {
                clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            }
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = BotTexts.GREETING,
                replyMarkup = keyboardMarkup
            )
        }
        command("help") {
            val chatId = message.chat.id
            if (!clients.containsKey(chatId)) {
                clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            }
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = BotTexts.HELP_MESSAGE
            )
        }
        command("restart") {
            val chatId = message.chat.id
            clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            bot.sendMessage(chatId = ChatId.fromId(chatId), text = BotTexts.RESTARTED)
        }
        command("find") {
            val chatId = message.chat.id
            clients[chatId] = ClientInfo(Mode.START, "", "", ArrayList(), 0)
            val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Определить мою текущую локацию (для мобильных устройств)",
                        callbackData = "from_location"
                    )
                ),
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Ввести локации вручную",
                        callbackData = "two_different"
                    )
                )
            )
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = BotTexts.FIND_TEXT,
                replyMarkup = inlineKeyboardMarkup
            )
        }
    }
}
