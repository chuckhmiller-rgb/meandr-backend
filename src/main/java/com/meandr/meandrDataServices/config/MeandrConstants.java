package com.meandr.meandrDataServices.config;

import java.util.Set;

public class MeandrConstants {
    
    public static final Set<String> EXCLUDED_PLACE_TYPES = Set.of(
            "veterinary_care", "hospital", "doctor",
            "dentist", "pharmacy", "lawyer", "real_estate_agency",
            "insurance_agency", "bank", "atm", "police", "funeral_home",
            "physiotherapist", "accounting", "storage", "moving_company",
            "car_dealer", "car_repair", "car_wash", "laundry", "post_office", "restaurant",
            "spa", "beauty_salon", "hair_salon", "nail_salon",
            "gym", "fitness_center", "health",
            "store", "shop", "shopping_mall", "supermarket", "grocery_store",
            "clothing_store", "furniture_store", "electronics_store",
            "home_goods_store", "hardware_store"
    );
}