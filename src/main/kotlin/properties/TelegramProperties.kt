package properties

import bot.NominatimBot
import java.io.IOException
import java.util.Properties

class TelegramProperties {
    companion object {

        private fun getProperties(from: String): Properties {
            val prop = Properties()
            try {
                prop.load(NominatimBot::class.java.getResourceAsStream(from))
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
            return prop
        }

        val telegramProperties: Properties
            get() {
                return getProperties("/telegram.properties")
            }
    }
}