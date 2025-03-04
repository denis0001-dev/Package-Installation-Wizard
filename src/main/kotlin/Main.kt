@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import ru.morozovit.PackageInstallationWizard.Companion.logger
import ru.morozovit.logging.Logger
import ru.morozovit.logging.Loglevel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class PackageInstallationWizard : Application() {
    companion object {
        val logger = Logger("piw.log", true, true)
    }

    // UI Components
    private lateinit var mainStage: Stage
    private lateinit var mainPane: StackPane

    // Pages
    private val page1 = BorderPane()
    private val page2 = BorderPane()
    private val page3 = BorderPane()
    private val page4 = BorderPane()

    // Global variables
    private val boldFont = Font.font("System", FontWeight.BOLD, 12.0)
    private var selected = ""
    private lateinit var nextButton: Button
    private val results = mutableListOf<String>()
    private lateinit var searchEntry: TextField
    private lateinit var installEntry: TextField
    private lateinit var searchListView: ListView<String>
    private lateinit var installListView: ListView<String>
    private lateinit var errorLabel: Label
    private lateinit var infoFrame: HBox

    private val requiredUtilities = mapOf(
        "apt-cache".duplicate(),
        "apt-rdepends".duplicate(),
        "grep-available" to "dctrl-tools",
        "grep-status" to "dctrl-tools"
    )

    private inline fun checkRequiredUtilities(): Boolean {
        val missingUtilities = requiredUtilities.filter {
            logger.log(Loglevel.DEBUG, "Checking for ${it.key}, actual package name ${it.value}")
            !isUtilityInstalled(it.key)
        }

        if (missingUtilities.isNotEmpty()) {
            logger.log(Loglevel.WARN, "Missing utilities: ${missingUtilities.keys.joinToString(", ")}")
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Missing Utilities"
            alert.headerText = "Some required utilities are missing"
            alert.contentText =
                """
                    |The following utilities are required but not installed:
                    |${missingUtilities.keys.joinToString("\n")}
                    |
                    |Do you want to install them now?
                """.trimMargin()

            val result = alert.showAndWait()
            if (result.get() == ButtonType.OK) {
                return installMissingUtilities(missingUtilities.values.toSet())
            }
            return false
        }
        return true
    }

    private fun isUtilityInstalled(utility: String): Boolean {
        val process = ProcessBuilder("which", utility)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        return process.waitFor() == 0
    }

    private fun installMissingUtilities(utilities: Collection<String>): Boolean {
        try {
            val process = ProcessBuilder("pkexec", "apt-get", "install", "-y", *utilities.toTypedArray())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw Exception("Installation failed with exit code $exitCode")
            }
            return true
        } catch (e: Exception) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Installation Error"
            alert.headerText = "Failed to install required utilities"
            alert.contentText = "Error: ${e.message}"
            alert.showAndWait()
            return false
        }
    }

    private fun showErrorAndExit(message: String) {
        logger.log(Loglevel.FATAL, "Cannot start the application. $message")
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Error"
        alert.headerText = "Cannot start the application"
        alert.contentText = message
        alert.showAndWait()
        Platform.exit()
    }

    // Add this function to create a header
    private fun createHeader(): HBox {
        val headerBox = HBox()
        headerBox.style = "-fx-border-color: black; -fx-border-width: 0 0 2 0;"
        headerBox.padding = Insets(5.0)

        val headerLabel = Label("Installation Wizard")
        headerLabel.font = boldFont
        headerLabel.alignment = Pos.CENTER_LEFT
        headerLabel.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(headerLabel, Priority.ALWAYS)

        val installerImage = Image(javaClass.getResourceAsStream("/installer_2.png"))
        val imageView = javafx.scene.image.ImageView(installerImage)
        imageView.fitHeight = 64.0
        imageView.fitWidth = 64.0

        headerBox.children.addAll(headerLabel, imageView)
        return headerBox
    }

    override fun start(stage: Stage) {
        logger.log(Loglevel.INFO, "Starting application")
        logger.log(Loglevel.INFO, "Checking for required utilities")
        logger.log(Loglevel.DEBUG, "Checking for apt")
        if (!File("/usr/bin/apt").exists()) {
            showErrorAndExit("This application requires the apt package manager, which is not found on this system.")
            return
        } else {
            logger.log(Loglevel.DEBUG, "Apt exists!")
        }

        logger.log(Loglevel.DEBUG, "Checking the required apt packages")
        if (!checkRequiredUtilities()) {
            showErrorAndExit("Required utilities are not installed. The application cannot continue.")
            return
        }
        logger.log(Loglevel.INFO, "Required utilities installed, starting the application")

        mainStage = stage
        mainStage.title = "Package Installation Wizard"
        mainStage.isResizable = false

        mainPane = StackPane()

        val scene = Scene(mainPane, 700.0, 512.0)
        mainStage.scene = scene

        // Set up close request handler
        mainStage.setOnCloseRequest { event ->
            event.consume()
            cancelInstall()
        }

        // Show first page
        showPage1()

        mainStage.show()
    }

    private fun cancelInstall() {
        val cancelDialog = Stage()
        cancelDialog.title = "Cancel"

        val dialogVBox = VBox(10.0)
        dialogVBox.alignment = Pos.CENTER
        dialogVBox.padding = Insets(10.0)

        val cancelLabel = Label("Cancel the installation?")

        val buttonBox = HBox(10.0)
        buttonBox.alignment = Pos.CENTER

        val yesButton = Button("Yes")
        yesButton.setOnAction { Platform.exit() }

        val noButton = Button("No")
        noButton.setOnAction { cancelDialog.close() }

        buttonBox.children.addAll(yesButton, noButton)
        dialogVBox.children.addAll(cancelLabel, buttonBox)

        val dialogScene = Scene(dialogVBox, 300.0, 150.0)
        cancelDialog.scene = dialogScene
        cancelDialog.show()
    }

    private fun createBottomButtons(
        parent: BorderPane,
        nextAction: () -> Unit = {},
        backAction: () -> Unit = {},
        cancelDisabled: Boolean = false,
        nextDisabled: Boolean = false,
        backDisabled: Boolean = false
    ): Triple<Button, Button, Button> {
        val buttonBox = HBox(5.0)
        buttonBox.alignment = Pos.CENTER_RIGHT
        buttonBox.padding = Insets(5.0)

        val cancelButton = Button("Cancel")
        cancelButton.setOnAction { cancelInstall() }
        cancelButton.isDisable = cancelDisabled

        nextButton = Button("Next >")
        nextButton.setOnAction { nextAction() }
        nextButton.isDisable = nextDisabled

        val backButton = Button("< Back")
        backButton.setOnAction { backAction() }
        backButton.isDisable = backDisabled

        buttonBox.children.addAll(backButton, nextButton, cancelButton)
        parent.bottom = buttonBox

        return Triple(cancelButton, nextButton, backButton)
    }

    private fun showPage1() {
        // Clear the main pane and add page1
        mainPane.children.clear()
        page1.children.clear()

        // Left side - Installer image
        val imagePane = StackPane()
        val installerImage = Image(javaClass.getResourceAsStream("/installer.png"))
        val imageView = javafx.scene.image.ImageView(installerImage)
        imageView.fitHeight = 512.0
        imageView.fitWidth = 290.0
        imageView.isPreserveRatio = true
        imagePane.children.add(imageView)
        page1.left = imagePane

        // Right side - Text content
        val textBox = VBox(10.0)
        textBox.padding = Insets(10.0)

        val titleLabel = Label("Welcome to the Package Installation Wizard")
        titleLabel.font = boldFont

        val descLabel = Label("This wizard will guide you through the package installation process.")
        descLabel.isWrapText = true
        descLabel.prefWidth = 380.0

        val instructionLabel = Label("Click \"Next\" to get started.")

        textBox.children.addAll(titleLabel, descLabel, instructionLabel)
        page1.center = textBox

        // Bottom buttons
        createBottomButtons(
            parent = page1,
            nextAction = { showPage2() },
            backDisabled = true
        )

        mainPane.children.add(page1)
    }

    private fun showPage2() {
        // Clear the main pane and add page2
        mainPane.children.clear()
        page2.children.clear()

        // Header
        page2.top = createHeader()

        // Main content
        val contentBox = VBox(10.0)
        contentBox.padding = Insets(10.0)

        val selectionLabel = Label("Select the action you want to perform:")
        selectionLabel.font = boldFont

        val radioGroup = ToggleGroup()

        val installRadio = RadioButton("Install package(s)")
        installRadio.toggleGroup = radioGroup
        installRadio.setOnAction {
            selected = "install"
            nextButton.isDisable = false
        }

        val removeRadio = RadioButton("Remove package(s)")
        removeRadio.toggleGroup = radioGroup
        removeRadio.setOnAction {
            selected = "remove"
            nextButton.isDisable = false
        }

        val searchRadio = RadioButton("Search for package(s)")
        searchRadio.toggleGroup = radioGroup
        searchRadio.setOnAction {
            selected = "search"
            nextButton.isDisable = false
        }

        contentBox.children.addAll(selectionLabel, installRadio, removeRadio, searchRadio)
        page2.center = contentBox

        // Bottom buttons
        val (_, next, _) = createBottomButtons(
            parent = page2,
            nextAction = { showPage3() },
            backAction = { showPage1() },
            nextDisabled = true
        )
        nextButton = next

        mainPane.children.add(page2)
    }

    private fun showPage3() {
        // Clear the main pane and add page3
        mainPane.children.clear()
        page3.children.clear()

        page3.top = createHeader()

        // Main content based on selection
        val contentBox = VBox(10.0)
        contentBox.padding = Insets(10.0)

        when (selected) {
            "install" -> setupInstallContent(contentBox)
            "search" -> setupSearchContent(contentBox)
            "remove" -> setupRemoveContent(contentBox)
        }

        page3.center = contentBox

        // Bottom buttons
        val (_, next, _) = createBottomButtons(
            parent = page3,
            nextAction = { showPage4() },
            backAction = { showPage2() },
            nextDisabled = selected != "search"
        )
        nextButton = next

        mainPane.children.add(page3)
    }

    private fun setupInstallContent(contentBox: VBox) {
        infoFrame = HBox(10.0)
        infoFrame.alignment = Pos.CENTER_LEFT

        val packageLabel = Label("Package: ")

        installEntry = TextField()

        val addButton = Button("Add")
        addButton.setOnAction { installAdd() }

        val removeButton = Button("Remove selected")
        removeButton.setOnAction { removeActive() }

        errorLabel = Label("ERROR: Package not found!")
        errorLabel.style = "-fx-text-fill: red;"
        errorLabel.isVisible = false

        infoFrame.children.addAll(packageLabel, installEntry, addButton, removeButton, errorLabel)

        installListView = ListView()
        installListView.prefHeight = 300.0
        VBox.setVgrow(installListView, Priority.ALWAYS)

        contentBox.children.addAll(infoFrame, installListView)
    }

    private fun setupSearchContent(contentBox: VBox) {
        val searchFrame = HBox(10.0)
        searchFrame.alignment = Pos.CENTER_LEFT

        val queryLabel = Label("Query: ")

        searchEntry = TextField()

        val searchButton = Button("Search")
        searchButton.setOnAction { search() }

        searchFrame.children.addAll(queryLabel, searchEntry, searchButton)

        searchListView = ListView()
        searchListView.prefHeight = 300.0
        VBox.setVgrow(searchListView, Priority.ALWAYS)

        contentBox.children.addAll(searchFrame, searchListView)
    }

    private fun setupRemoveContent(contentBox: VBox) {
        // Implement remove functionality here
        val label = Label("Remove functionality not implemented yet")
        contentBox.children.add(label)
    }

    private fun showPage4() {
        // Clear the main pane and add page4
        mainPane.children.clear()
        page4.children.clear()

        // Header
        page4.top = createHeader()

        // Main content based on selection
        val contentBox = VBox(10.0)
        contentBox.padding = Insets(10.0)

        when (selected) {
            "install" -> setupInstallOptions(contentBox)
            "search" -> contentBox.children.add(Label("Search results processing"))
            "remove" -> contentBox.children.add(Label("Remove processing"))
        }

        page4.center = contentBox

        // Bottom buttons
        createBottomButtons(
            parent = page4,
            backAction = { showPage3() }
        )

        mainPane.children.add(page4)
    }

    private fun setupInstallOptions(contentBox: VBox) {
        val label = Label("Installation options")
        label.font = boldFont

        val packagesToInstall = Label("Packages to install:")

        val packagesList = ListView<String>()
        packagesList.items.addAll(installListView.items)

        contentBox.children.addAll(label, packagesToInstall, packagesList)
    }

    private fun search() {
        searchListView.items.clear()

        if (searchEntry.text.isEmpty()) {
            searchListView.items.add("Empty query.")
            return
        }

        CompletableFuture.runAsync {
            try {
                val process = ProcessBuilder("apt-cache", "search", searchEntry.text)
                    .redirectErrorStream(true)
                    .start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val results = mutableListOf<String>()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    line?.let { results.add(it) }
                }

                process.waitFor()

                Platform.runLater {
                    if (results.isEmpty()) {
                        searchListView.items.add("No results found.")
                    } else {
                        searchListView.items.addAll(results)
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    searchListView.items.add("Error: ${e.message}")
                }
            }
        }
    }

    private fun installAdd() {
        errorLabel.isVisible = false

        if (installEntry.text.isEmpty()) return

        CompletableFuture.runAsync {
            try {
                val packageName = installEntry.text
                val process = ProcessBuilder("apt-cache", "show", packageName)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

                val exitCode = process.waitFor()

                Platform.runLater {
                    if (exitCode == 0) {
                        // Package exists
                        if (!installListView.items.contains(packageName)) {
                            installListView.items.add(packageName)
                        }
                        installEntry.clear()

                        // Enable next button if we have at least one package
                        if (installListView.items.isNotEmpty()) {
                            nextButton.isDisable = false
                        }
                    } else {
                        // Package not found
                        errorLabel.isVisible = true
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    errorLabel.text = "ERROR: ${e.message}"
                    errorLabel.isVisible = true
                }
            }
        }
    }

    private fun removeActive() {
        val selectedIndex = installListView.selectionModel.selectedIndex
        if (selectedIndex >= 0) {
            installListView.items.removeAt(selectedIndex)

            // Disable next button if no packages left
            if (installListView.items.isEmpty()) {
                nextButton.isDisable = true
            }
        }
    }

    private fun getDependencies(packageName: String): Map<String, List<String>> {
        val dependencies = mutableListOf<String>()
        val recommends = mutableListOf<String>()
        val suggests = mutableListOf<String>()
        val breaks = mutableListOf<String>()
        val conflicts = mutableListOf<String>()

        try {
            // Get dependencies
            val depsProcess = ProcessBuilder("sh", "-c",
                "echo \$(apt-rdepends $packageName | grep -v \"^ \" | grep -v \"^libc-dev\$\")")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            val depsReader = BufferedReader(InputStreamReader(depsProcess.inputStream))
            val depsOutput = depsReader.readLine() ?: ""
            val deps = depsOutput.split(" ").toMutableList()

            // Filter out already installed packages
            val filteredDeps = deps.filter { dep ->
                var packageToCheck = dep
                if (packageToCheck.endsWith(":any")) {
                    packageToCheck = packageToCheck.removeSuffix(":any")
                }

                // Check for virtual packages
                val virtProcess = ProcessBuilder("sh", "-c",
                    "grep-available -F Provides -s Package $packageToCheck | sed 's/Package: //'")
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()

                val virtReader = BufferedReader(InputStreamReader(virtProcess.inputStream))
                val virtOutput = virtReader.readLine()
                if (!virtOutput.isNullOrEmpty()) {
                    packageToCheck = virtOutput
                }

                // Check if package is already installed
                val installedProcess = ProcessBuilder("dpkg", "-s", packageToCheck)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()

                installedProcess.waitFor() != 0
            }

            dependencies.addAll(filteredDeps)

            // Get conflicts
            val conflictsProcess = ProcessBuilder("sh", "-c",
                "grep-status -P $packageName --exact-match -s Conflicts | sed -e 's/Conflicts: //' | tr ',' ' ' | xargs")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            val conflictsReader = BufferedReader(InputStreamReader(conflictsProcess.inputStream))
            val conflictsOutput = conflictsReader.readLine() ?: ""
            conflicts.addAll(conflictsOutput.split(" ").filter { it.isNotEmpty() })

            // Get recommends
            val recommendsProcess = ProcessBuilder("sh", "-c",
                "grep-status -P $packageName --exact-match -s Recommends | sed -e 's/Recommends: //' -e 's/|//' -e 's/([^()]*)//g' | tr ',' ' ' | xargs")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            val recommendsReader = BufferedReader(InputStreamReader(recommendsProcess.inputStream))
            val recommendsOutput = recommendsReader.readLine() ?: ""
            recommends.addAll(recommendsOutput.split(" ").filter { it.isNotEmpty() })

            // Get suggests
            val suggestsProcess = ProcessBuilder("sh", "-c",
                "grep-status -P $packageName --exact-match -s Suggests | sed -e 's/Suggests: //' -e 's/,//'")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            val suggestsReader = BufferedReader(InputStreamReader(suggestsProcess.inputStream))
            val suggestsOutput = suggestsReader.readLine() ?: ""
            suggests.addAll(suggestsOutput.split(" ").filter { it.isNotEmpty() })

            // Get breaks
            val breaksProcess = ProcessBuilder("sh", "-c",
                "grep-status -P $packageName --exact-match -s Breaks | sed -e 's/Breaks: //' -e 's/|//' -e 's/([^()]*)//g' | tr ',' ' ' | xargs")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            val breaksReader = BufferedReader(InputStreamReader(breaksProcess.inputStream))
            val breaksOutput = breaksReader.readLine() ?: ""
            breaks.addAll(breaksOutput.split(" ").filter { it.isNotEmpty() })

        } catch (e: Exception) {
            println("Error getting dependencies: ${e.message}")
        }

        return mapOf(
            "dependencies" to dependencies,
            "recommends" to recommends,
            "suggests" to suggests,
            "breaks" to breaks,
            "conflicts" to conflicts
        )
    }
}

fun main() {
    try {
        Application.launch(PackageInstallationWizard::class.java)
        logger.log(Loglevel.INFO, "Exiting")
    } catch (e: Exception) {
        logger.log(Loglevel.FATAL, "An unexpected error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}