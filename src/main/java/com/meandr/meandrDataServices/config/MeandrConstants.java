package com.meandr.meandrDataServices.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MeandrConstants {
    
    public static final Set<String> EXCLUDED_PLACE_TYPES = Set.of(
            "veterinary_care", "hospital", "doctor",
            "dentist", "pharmacy", "lawyer", "real_estate_agency",
            "insurance_agency", "bank", "atm", "police", "funeral_home",
            "physiotherapist", "accounting", "storage", "moving_company",
            "car_dealer", "car_repair", "car_wash", "laundry", "post_office", "restaurant",
            "spa", "beauty_salon", "hair_salon", "nail_salon",
            "gym", "fitness_center",
            "store", "shopping_mall", "supermarket", "grocery_store",
            "clothing_store", "furniture_store", "electronics_store",
            "home_goods_store", "hardware_store"
            //, "apartments", "shop", "health", "notary", "health", "apartment"
    );
    
    public static final List<String> MOST_RELEVANT_TYPES = List.of(
            // Most specific natural/outdoor types first
            "national_park", "state_park", "wildlife_refuge", "wildlife_park",
            "botanical_garden", "dog_park", "campground", "rv_park",
            "hiking_area", "picnic_ground", "off_roading_area", "adventure_sports_center",
            // Water
            "beach", "marina",
            // Animals
            "zoo", "aquarium",
            // History & Culture (specific before generic)
            "castle", "monument", "sculpture",
            "history_museum", "art_museum", "museum", "art_gallery",
            "historical_landmark", "cultural_landmark", "historical_place",
            "performing_arts_theater", "library",
            // Food & Drink (specific before generic)
            "brewery", "brewpub", "beer_garden", "vineyard",
            "cocktail_bar", "bar", "night_club",
            "bakery", "coffee_shop", "cafe",
            "fast_food_restaurant", "restaurant",
            "market",
            // Entertainment
            "amusement_park", "observation_deck",
            // Civic
            "university", "stadium", "sports_complex",
            "courthouse", "city_hall", "plaza", "town_square",
            // Worship
            "church", "synagogue", "mosque", "hindu_temple",
            // Rest & Refuel
            "lodging", "gas_station", "convenience_store", "rest_stop",
            // Broad types last
            "tourist_attraction", "park", "city_park", "natural_feature"
    );

    public static final Map<String, String> ENTITY_KEYWORDS = Map.ofEntries(
            Map.entry("WATERFALL", "waterfall falls cascade"),
            Map.entry("CLIMBING_CRAG", "climbing crag rock face"),
            Map.entry("BOULDER", "bouldering boulder"),
            Map.entry("HOT_SPRING", "hot spring thermal spring"),
            Map.entry("BIRD_HIDE", "bird hide birding wildlife blind"),
            Map.entry("SWIMMING_HOLE", "swimming hole swimming creek"),
            Map.entry("CAVE", "cave cavern grotto"),
            Map.entry("DARK_SKY_AREA", "dark sky stargazing"),
            Map.entry("SCENIC_OVERLOOK", "scenic overlook viewpoint vista"),
            Map.entry("PEAK", "mountain peak summit"),
            Map.entry("RIVER_ACCESS", "river access boat launch canoe kayak"),
            Map.entry("GHOST_TOWN", "ghost town abandoned"),
            Map.entry("ARCHAEOLOGICAL_SITE", "archaeological site ruins excavation"),
            Map.entry("TRAILHEAD", "trailhead trail access hiking"),
            Map.entry("OBSERVATORY", "observatory telescope stargazing"),
            Map.entry("HISTORIC_DISTRICT", "historic district"),
            Map.entry("CASTLE", "castle fort fortress"),
            Map.entry("WILDERNESS_HUT", "wilderness hut backcountry shelter cabin"),
            Map.entry("FISHING_SPOT", "fishing spot fishing access"),
            Map.entry("SPRING", "natural spring freshwater spring"),
            Map.entry("RUINS", "ruins abandoned historic"),
            Map.entry("ARTWORK", "public art sculpture mural"),
            Map.entry("DISTILLERY", "distillery whiskey bourbon spirits"),
            Map.entry("FARMERS_MARKET", "farmers market local market"),
            Map.entry("MILESTONE", "historic marker milestone"),
            Map.entry("WAYSIDE_SHRINE", "wayside shrine roadside shrine")
    );
}