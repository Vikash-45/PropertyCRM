package com.propertycrm.app.data.local

import androidx.room.TypeConverter
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.PropertyType

class Converters {

    @TypeConverter
    fun fromPropertyType(value: PropertyType?): String {
        return value?.name ?: PropertyType.OTHER.name
    }

    @TypeConverter
    fun toPropertyType(value: String?): PropertyType {
        return try {
            if (value != null) PropertyType.valueOf(value) else PropertyType.OTHER
        } catch (e: Exception) {
            PropertyType.OTHER
        }
    }

    @TypeConverter
    fun fromCustomerStage(value: CustomerStage?): String {
        return value?.name ?: CustomerStage.NEW.name
    }

    @TypeConverter
    fun toCustomerStage(value: String?): CustomerStage {
        return try {
            if (value != null) CustomerStage.valueOf(value) else CustomerStage.NEW
        } catch (e: Exception) {
            CustomerStage.NEW
        }
    }

    @TypeConverter
    fun fromFollowUpStatus(value: FollowUpStatus?): String {
        return value?.name ?: FollowUpStatus.PENDING.name
    }

    @TypeConverter
    fun toFollowUpStatus(value: String?): FollowUpStatus {
        return try {
            if (value != null) FollowUpStatus.valueOf(value) else FollowUpStatus.PENDING
        } catch (e: Exception) {
            FollowUpStatus.PENDING
        }
    }
}
