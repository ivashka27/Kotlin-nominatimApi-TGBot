package algo

import data.remote.models.Place
import data.remote.models.PlaceItem
import kotlin.math.*

class PubsHandler {

    data class Vertex(
        val lat: Double,
        val lon: Double,
        val name: String,
        val priority: Double
    ) {
        fun distanceTo(other: Vertex): Double {
            val dLat = Math.toRadians(other.lat - lat)
            val dLon = Math.toRadians(other.lon - lon)
            val a = sin(dLat / 2).pow(2) +
                    sin(dLon / 2).pow(2) *
                    cos(Math.toRadians(lat)) *
                    cos(Math.toRadians(other.lat))
            val rad = 6371.0
            val c = 2 * asin(sqrt(a))
            return rad * c
        }
    }

    private fun prepareData(places: Place): ArrayList<Vertex> {
        val res: ArrayList<Vertex> = places.map {
            Vertex(it.lat.toDouble(), it.lon.toDouble(), it.display_name, it.importance)
        } as ArrayList<Vertex>
        return res
    }

    fun findPath(places: Place, start: PlaceItem): ArrayList<Vertex> {
        val g = prepareData(places)
        g.sortBy { it.priority }
        g.reverse()
        val startVertex = Vertex(start.lat.toDouble(), start.lon.toDouble(), start.display_name, start.importance)
        g.sortBy { startVertex.distanceTo(it) }
        return g
    }


}