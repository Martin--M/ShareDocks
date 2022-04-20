def get_city_info_list(path):
    city_info_list = []
    with open(path) as csv_file:
        # Skip headers
        csv_file.readline()
        entries = csv_file.readlines()
        for entry in entries:
            values = entry.split(",")
            assert len(values) >= 6
            city_info = {
                "country": values[0],
                "name": values[1],
                "url": values[2],
                "lat": values[3],
                "lon": values[4],
                "id": values[5] }
            city_info_list.append(city_info)
    return city_info_list

def get_city_utils_string(city_info_list):
    file_string = """
package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng
import java.net.URL

object CityUtils {

    var currentCity: Int = 0

    class CityInfo(
        val name: String,
        val location: LatLng,
        val baseUrl: URL
    )
    val map: Map<Int, CityInfo> = mapOf(
"""

    for city_info in city_info_list:
        file_string += "        "
        file_string += city_info["id"]
        file_string += " to CityInfo(\""
        file_string += city_info["name"]
        file_string += "\", LatLng("
        file_string += city_info["lat"]
        file_string += ", "
        file_string += city_info["lon"]
        file_string += "), URL(\""
        file_string += city_info["url"]
        file_string += "\")),\n"

    file_string += """
    )
}
"""
    return file_string

def get_resource_array_string(city_info_list):
    file_string = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="cityData">
"""
    for city_info in city_info_list:
        file_string += "        <item>"
        file_string += city_info["country"]
        file_string += " - "
        file_string += city_info["name"]
        file_string += "</item>\n"

    file_string +="""
    </string-array>

    <string-array name="cityId">
"""
    for city_info in city_info_list:
        file_string += "        <item>"
    file_string += city_info["id"]
    file_string += "</item>\n"

    file_string += """
    </string-array>
</resources>
"""
    return file_string

if __name__ == "__main__":
    city_info_list_path = "../info/city_list.csv"
    city_utils_path = "src/main/java/com/martinm/sharedocks/CityUtils.kt"
    resource_array_path = "src/main/res/values/cityInfo.xml"

    city_list = get_city_info_list(city_info_list_path)
    city_utils_string = get_city_utils_string(city_list)
    resource_array_string = get_resource_array_string(city_list)

    with open(city_utils_path, "w") as city_utils_file:
        city_utils_file.write(city_utils_string)

    with open(resource_array_path, "w") as resource_array_file:
        resource_array_file.write(resource_array_string)
