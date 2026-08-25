package com.thorfinn;

import java.util.concurrent.Callable;

import com.thorfinn.orchestrator.Orchestrator;
import com.thorfinn.utils.VersionInfo;
import com.thorfinn.verification.PocApprovalMode;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "java -jar Thorfinn.jar",
        headerHeading = "\n",
        synopsisHeading = "Usage:\n  ",
        customSynopsis = "java -jar Thorfinn.jar <package-name> --config <path> [options]",
        parameterListHeading = "\nArguments:\n",
        optionListHeading = "\nOptions:\n",
        footerHeading = "\nExamples:\n",
        footer = {
            "  java -jar Thorfinn.jar com.example.app --config /path/to/config.yml",
            "  java -jar Thorfinn.jar com.example.app -c ./config/config.yml --time-limit 600",
            "  java -jar Thorfinn.jar com.example.app -c ./config/config.yml --auto-approve",
            "  java -jar Thorfinn.jar com.example.app -c ./config/config.yml --report-path ./thorfinn_report.json",
            "  java -jar Thorfinn.jar com.example.app -c ./config/config.yml --diff-report-path ./thorfinn_report.json",
            "  java -jar Thorfinn.jar com.example.app -c ./config/config.yml --skip-verify"
        },
        versionProvider = Main.VersionProvider.class,
        sortOptions = false,
        usageHelpWidth = 160
)
public class Main implements Callable<Integer> {

    private static final int DEFAULT_TIME_LIMIT = 30000;

    @Parameters(index = "0", paramLabel = "<package-name>", description = "Android package name of the target app (must be installed on connected device)")
    private String packageName;

    @Option(names = {"-c", "--config"}, paramLabel = "<path>", required = true, description = "Path to config.yml (required)")
    private String configPath;

    @Option(names = {"-t", "--time-limit"}, paramLabel = "<seconds>", description = "Time limit for CPG/taint analysis (default: 300)")
    private int timeLimit = DEFAULT_TIME_LIMIT;

    @Option(names = {"-r", "--report-path"}, paramLabel = "<path>", description = "Path to previous report to reuse POCs and skip redundant LLM calls")
    private String reportPath;

    @Option(names = {"-d", "--diff-report-path"}, paramLabel = "<path>", description = "Path to previous report to generate a diff containing only new findings")
    private String diffReportPath;

    @Option(names = {"-y", "--auto-approve"}, description = "Auto-approve all LLM-generated POC commands without prompting")
    private boolean autoApprove;

    @Option(names = {"-s", "--skip-verify"}, description = "Skip execution of all LLM-generated POC commands")
    private boolean skipVerify;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "Print the Thorfinn version and exit")
    private boolean versionRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message")
    private boolean helpRequested;

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        cmd.getCommandSpec().usageMessage().header("Thorfinn " + VersionInfo.getVersion() + " - Automated Android Client-Side Security Scanner\n");
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        boolean diffMode = false;
        String finalReportPath = null;

        if (diffReportPath != null) {
            finalReportPath = diffReportPath;
            diffMode = true;
        } else if (reportPath != null) {
            finalReportPath = reportPath;
        }

        PocApprovalMode pocMode = PocApprovalMode.INTERACTIVE;
        if (autoApprove) {
            pocMode = PocApprovalMode.AUTO_APPROVE;
        }
        if (skipVerify) {
            pocMode = PocApprovalMode.SKIP;
        }

        Orchestrator orchestrator = new Orchestrator();
        orchestrator.execute(packageName, timeLimit, configPath, pocMode, finalReportPath, diffMode);
        return 0;
    }

    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{"Thorfinn " + VersionInfo.getVersion()};
        }
    }
}
