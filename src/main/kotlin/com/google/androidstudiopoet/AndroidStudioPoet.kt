/*
 *  Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.androidstudiopoet

import com.google.androidstudiopoet.converters.ConfigPojoToBuildTypeConfigsConverter
import com.google.androidstudiopoet.converters.ConfigPojoToFlavourConfigsConverter
import com.google.androidstudiopoet.models.ConfigPOJO
import com.google.androidstudiopoet.models.ProjectBlueprint
import com.google.androidstudiopoet.writers.SourceModuleWriter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.intellij.lang.annotations.Language
import java.awt.BorderLayout
import java.awt.Color
import java.awt.EventQueue
import java.awt.Font
import java.io.File
import javax.swing.*
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.border.EmptyBorder
import kotlin.system.measureTimeMillis

class AndroidStudioPoet(private val modulesWriter: SourceModuleWriter, private val filename: String?,
                        private val configPojoToFlavourConfigsConverter: ConfigPojoToFlavourConfigsConverter,
                        private val configPojoToBuildTypeConfigsConverter: ConfigPojoToBuildTypeConfigsConverter) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AndroidStudioPoet(Injector.modulesWriter, args.firstOrNull(),
                    Injector.configPojoToFlavourConfigsConverter,
                    Injector.configPojoToBuildTypeConfigsConverter).run()
        }

        @Language("JSON") val SAMPLE_CONFIG = """
            {
              "projectName": "genny",
              "root": "./modules/",
              "gradleVersion": "4.3.1",
              "androidGradlePluginVersion": "3.0.1",
              "kotlinVersion": "1.1.60",
              "numModules": "5",
              "allMethods": "4000",
              "javaPackageCount": "20",
              "javaClassCount": "8",
              "javaMethodCount": "2000",
              "kotlinPackageCount": "20",
              "kotlinClassCount": "8",
              "androidModules": "2",
              "numActivitiesPerAndroidModule": "8",
              "productFlavors": [
                  2, 3
               ],
               "topologies": [
                  {"type": "random", "seed": "2"}
               ],
              "dependencies": [
                {"from": 3, "to": 2},
                {"from": 4, "to": 2},
                {"from": 4, "to": 3}
              ],
              "buildTypes": 6
            }
            """.trimIndent()
    }

    fun run() {

        val configPOJO = fromFile(filename)
        when (configPOJO) {
            null -> showUI(SAMPLE_CONFIG)
            else -> processInput(configPOJO)
        }
    }

    private fun showUI(jsonText: String) {
        EventQueue.invokeLater {
            try {
                val frame = createUI(jsonText)
                frame.isVisible = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createUI(jsonText: String): JFrame {
        val frame = JFrame()
        val textArea = createTextArea(jsonText)
        val scrollPane = JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS)

        val btnGenerate = JButton("Generate").apply {
            addActionListener {
                try {
                    val text = textArea.text
                    println(text)
                    processInput(configFrom(text)!!)

                } catch (e: Exception) {
                    println("ERROR: the generation failed due to JSON script errors - " +
                            "please fix and try again ")
                }
            }
        }

        val contentPane = JPanel().apply {
            border = EmptyBorder(5, 5, 5, 5)
            layout = BorderLayout(0, 0)
            add(createTitleLabel(), BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            add(btnGenerate, BorderLayout.SOUTH)
        }

        frame.defaultCloseOperation = EXIT_ON_CLOSE

        frame.contentPane = contentPane

        frame.pack()
        return frame
    }

    private fun processInput(configPOJO: ConfigPOJO) {
        var projectBluePrint: ProjectBlueprint? = null
        val timeSpent = measureTimeMillis {
            projectBluePrint = ProjectBlueprint(configPOJO, configPojoToFlavourConfigsConverter, configPojoToBuildTypeConfigsConverter)
            modulesWriter.generate(projectBluePrint!!)
        }
        println("Finished in $timeSpent ms")
        println("Dependency graph:")
        projectBluePrint!!.printDependencies()
        if (projectBluePrint!!.hasCircularDependencies()) {
            println("WARNING: there are circular dependencies")
        }
    }

    private fun createTitleLabel(): JLabel {
        return JLabel("Android Studio Poet").apply {
            horizontalAlignment = SwingConstants.CENTER
        }
    }

    private fun createTextArea(jsonText: String): JTextArea {
        return JTextArea().apply {
            background = Color(46, 48, 50)
            foreground = Color.CYAN
            font = Font("Menlo", Font.PLAIN, 18)
            text = jsonText
            caretPosition = text.length
            caretColor = Color.YELLOW
            rows = 30
            columns = 50
        }
    }

    private fun fromFile(filename: String?): ConfigPOJO? = when {
        filename == null -> null
        !File(filename).canRead() -> null
        else -> File(filename).readText().let {
            return configFrom(it)
        }
    }


    private fun configFrom(json: String): ConfigPOJO? {

        val gson = Gson()

        try {
            return gson.fromJson(json, ConfigPOJO::class.java)
        } catch (js: JsonSyntaxException) {
            System.err.println("Cannot parse json: $js")
            return null
        }
    }
}