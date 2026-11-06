package com.propertycrm.app.data.local

import androidx.room.TypeConverter
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.NextAction
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType

class Converters {

    @TypeConverter
    fun fromPropertyType(value: PropertyType?): String = value?.name ?: PropertyType.OTHER.name

    @TypeConverter
    fun toPropertyType(value: String?): PropertyType = try {
        if (value != null) PropertyType.valueOf(value) else PropertyType.OTHER
    } catch (e: Exception) {
        PropertyType.OTHER
    }

    @TypeConverter
    fun fromCustomerStage(value: CustomerStage?): String = value?.name ?: CustomerStage.NEW.name

    @TypeConverter
    fun toCustomerStage(value: String?): CustomerStage = try {
        if (value != null) CustomerStage.valueOf(value) else CustomerStage.NEW
    } catch (e: Exception) {
        CustomerStage.NEW
    }

    @TypeConverter
    fun fromFollowUpStatus(value: FollowUpStatus?): String = value?.name ?: FollowUpStatus.PENDING.name

    @TypeConverter
    fun toFollowUpStatus(value: String?): FollowUpStatus = try {
        if (value != null) FollowUpStatus.valueOf(value) else FollowUpStatus.PENDING
    } catch (e: Exception) {
        FollowUpStatus.PENDING
    }

    @TypeConverter
    fun fromRequirementType(value: RequirementType?): String = value?.name ?: RequirementType.BUY.name

    @TypeConverter
    fun toRequirementType(value: String?): RequirementType = try {
        if (value != null) RequirementType.valueOf(value) else RequirementType.BUY
    } catch (e: Exception) {
        RequirementType.BUY
    }

    @TypeConverter
    fun fromNextAction(value: NextAction?): String = value?.name ?: NextAction.CALL.name

    @TypeConverter
    fun toNextAction(value: String?): NextAction = try {
        if (value != null) NextAction.valueOf(value) else NextAction.CALL
    } catch (e: Exception) {
        NextAction.CALL
    }

    @TypeConverter
    fun fromAppointmentStatus(value: AppointmentStatus?): String = value?.name ?: AppointmentStatus.SCHEDULED.name

    @TypeConverter
    fun toAppointmentStatus(value: String?): AppointmentStatus = try {
        if (value != null) AppointmentStatus.valueOf(value) else AppointmentStatus.SCHEDULED
    } catch (e: Exception) {
        AppointmentStatus.SCHEDULED
    }

    @TypeConverter
    fun fromInteractionType(value: InteractionType?): String = value?.name ?: InteractionType.SYSTEM.name

    @TypeConverter
    fun toInteractionType(value: String?): InteractionType = try {
        if (value != null) InteractionType.valueOf(value) else InteractionType.SYSTEM
    } catch (e: Exception) {
        InteractionType.SYSTEM
    }

    @TypeConverter
    fun fromCallOutcome(value: CallOutcome?): String? = value?.name

    @TypeConverter
    fun toCallOutcome(value: String?): CallOutcome? = try {
        if (value != null) CallOutcome.valueOf(value) else null
    } catch (e: Exception) {
        null
    }
}
