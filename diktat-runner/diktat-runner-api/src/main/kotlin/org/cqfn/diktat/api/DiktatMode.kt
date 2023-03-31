package org.cqfn.diktat.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mode of `diktat`
 */
@Serializable
enum class DiktatMode {
    @SerialName("check")
    CHECK,
    @SerialName("fix")
    FIX,
    ;
}
