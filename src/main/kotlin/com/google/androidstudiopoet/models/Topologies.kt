/*
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.google.androidstudiopoet.models

import java.security.InvalidParameterException
import java.util.*

/**
 * Enum with all supported topologies
 */
enum class Topologies {
    FULL {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val result = mutableListOf<DependencyConfig>()
            for (from in 0 until configPOJO.numModules) {
                for (to in from + 1 until configPOJO.numModules) {
                    result.add(DependencyConfig(from, to))
                }
            }
            return result
        }
    },

    RANDOM {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val random = randomWithSeed(parameters)
            val result = mutableListOf<DependencyConfig>()
            for (from in 0 until configPOJO.numModules) {
                for (to in from + 1 until configPOJO.numModules) {
                    if (random.nextBoolean()) {
                        result.add(DependencyConfig(from, to))
                    }
                }
            }
            return result
        }
    },

    RANDOM_CONNECTED {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val random = randomWithSeed(parameters)
            val result = mutableListOf<DependencyConfig>()
            var to = 1
            while (to < configPOJO.numModules) {
                var numFrom = 0
                for (from in 0 until to) {
                    if (random.nextBoolean()) {
                        result.add(DependencyConfig(from, to))
                        numFrom++
                    }
                }
                if (numFrom > 0) {
                    to++
                }
            }
            return result
        }
    },

    LINEAR {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> = (1 until configPOJO.numModules).map { DependencyConfig(it - 1, it) }
    },

    STAR {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> =
                (1 until configPOJO.numModules).map { DependencyConfig(0, it) }
    },

    BINARY_TREE {
        private fun getParent(node: Int) = ((node + 1) / 2) - 1

        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> =
                (1 until configPOJO.numModules).map { DependencyConfig(getParent(it), it) }
    },

    RECTANGLE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val width: Int = parameters["width"]?.toInt() ?: throw InvalidParameterException("No width was specified")
            if (width <= 0) {
                throw InvalidParameterException("width must be greater than 0 but $width was given")
            }
            val result = mutableListOf<DependencyConfig>()
            for (to in width until configPOJO.numModules) {
                val base = ((to / width) - 1) * width
                for (from in 0 until width) {
                    result.add(DependencyConfig(base + from, to))
                }
            }
            return result
        }
    },

    RANDOM_RECTANGLE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val width: Int = parameters["width"]?.toInt() ?: throw InvalidParameterException("width was not specified for $parameters")
            if (width <= 0) {
                throw InvalidParameterException("width must be greater than 0 on $parameters")
            }
            val random = randomWithSeed(parameters)

            val result = mutableListOf<DependencyConfig>()
            for (to in width until configPOJO.numModules) {
                val base = ((to / width) - 1) * width
                for (from in 0 until width) {
                    if (random.nextBoolean()) {
                        result.add(DependencyConfig(base + from, to))
                    }
                }
            }
            return result
        }
    },

    RANDOM_CONNECTED_RECTANGLE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig> {
            val width: Int = parameters["width"]?.toInt() ?: throw InvalidParameterException("width was not specified for $parameters")
            if (width <= 0) {
                throw InvalidParameterException("width must be greater than 0 on $parameters")
            }
            val random = randomWithSeed(parameters)

            val result = mutableListOf<DependencyConfig>()
            var to = width
            while (to < configPOJO.numModules) {
                val base = ((to / width) - 1) * width
                var numFrom = 0
                for (from in 0 until width) {
                    if (random.nextBoolean()) {
                        result.add(DependencyConfig(base + from, to))
                        numFrom++
                    }
                }
                if (numFrom > 0) {
                    to++
                }
            }
            return result
        }
    }
    ;

    protected fun randomWithSeed(parameters: Map<String, String>) : Random {
        val seedInput = parameters["seed"]
        val seed : Long = seedInput?.toLong() ?: 0
        return Random(seed)
    }

    /**
     * Function that should add dependencies to configPOJO based on the given parameters and the
     * content of configPOJO
     */
    abstract fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<DependencyConfig>
}