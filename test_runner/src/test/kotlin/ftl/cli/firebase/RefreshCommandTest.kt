package ftl.cli.firebase

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import ftl.test.util.FlankTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.runner.RunWith
import picocli.CommandLine

@RunWith(FlankTestRunner::class)
class RefreshCommandTest {
    @Rule
    @JvmField
    val systemOutRule: SystemOutRule = SystemOutRule().enableLog().muteForSuccessfulTests()

    @get:Rule
    val exit = ExpectedSystemExit.none()!!

    /** Create one result dir with matrix_ids.json for refresh command tests */
    private fun setupResultsDir() {
        val parent = "results/2018-09-07_01:21:14.201000_SUfE"
        val matrixIds = Paths.get(parent, "matrix_ids.json")
        val yamlCfg = Paths.get(parent, "flank.yml")
        matrixIds.parent.toFile().mkdirs()

        Runtime.getRuntime().addShutdownHook(Thread {
            File(parent).deleteRecursively()
        })

        if (matrixIds.toFile().exists() && yamlCfg.toFile().exists()) return

        Files.write(
            matrixIds, """
                {
                "matrix-1": {
                "matrixId": "matrix-1",
                "state": "FINISHED",
                "gcsPath": "1",
                "webLink": "https://console.firebase.google.com/project/mockProjectId/testlab/histories/1/matrices/1/executions/1",
                "downloaded": false,
                "billableVirtualMinutes": 1,
                "billablePhysicalMinutes": 0,
                "outcome": "success" }
            }
            """.trimIndent().toByteArray()
        )

        Files.write(
            yamlCfg, """
             gcloud:
               app: ../test_app/apks/app-debug.apk
               test: ../test_app/apks/app-debug-androidTest.apk
            """.trimIndent().toByteArray()
        )
    }

    @Test
    fun refreshCommandPrintsHelp() {
        val refresh = RefreshCommand()
        assertThat(refresh.usageHelpRequested).isFalse()
        CommandLine.run<Runnable>(refresh, System.out, "-h")

        val output = systemOutRule.log
        Truth.assertThat(output).startsWith(
            "Downloads results for the last Firebase Test Lab run\n" +
                    "\n" +
                    "refresh [-h]\n" +
                    "\n" +
                    "Description:\n" +
                    "\n" +
                    "Selects the most recent run in the results/ folder.\n" +
                    "Reads in the matrix_ids.json file. Refreshes any incomplete matrices.\n" +
                    "\n" +
                    "\n" +
                    "Options:\n" +
                    "  -h, --help   Prints this help message\n"
        )

        assertThat(refresh.usageHelpRequested).isTrue()
    }

    @Test
    fun refreshCommandRuns() {
        exit.expectSystemExit()
        setupResultsDir()
        val cmd = RefreshCommand()
        cmd.usageHelpRequested
        cmd.run()
        val output = systemOutRule.log
        Truth.assertThat(output).contains("1 / 1 (100.00%)")
    }

    @Test
    fun refreshCommandOptions() {
        val cmd = RefreshCommand()
        assertThat(cmd.usageHelpRequested).isFalse()
        cmd.usageHelpRequested = true
        assertThat(cmd.usageHelpRequested).isTrue()
    }
}
