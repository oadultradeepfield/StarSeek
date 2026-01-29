package com.oadultradeepfield.starseek.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ObjectType {
  STAR,
  NEBULA,
  GALAXY,
  CLUSTER;

  val displayName: String
    get() =
        when (this) {
          STAR -> "Stars"
          NEBULA -> "Nebulae"
          GALAXY -> "Galaxies"
          CLUSTER -> "Clusters"
        }
}
