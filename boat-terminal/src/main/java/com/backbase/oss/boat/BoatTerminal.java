package com.backbase.oss.boat;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import com.backbase.oss.boat.BoatTerminal.VersionProvider;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
    name = "boat-terminal",
    description = "Boat Terminal",
    versionProvider = VersionProvider.class,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    sortOptions = false,
    synopsisSubcommandLabel = "COMMAND",
    subcommands = {
        BundleCommand.class,
        ExportCommand.class,
        ExplodeCommand.class,
        CompletionCommand.class,
    })
public class BoatTerminal implements Runnable {

    static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            return new String[] {getClass().getPackage().getImplementationVersion()};
        }
    }

    public static void main(String[] args) {
        System.exit(new BoatTerminal().run(args));
    }

    private static final Logger ROOT = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    @Spec
    private CommandSpec spec;

    @Option(names = {"-v", "--verbose"}, order = 50, scope = ScopeType.INHERIT,
        description = "Verbose output; multiple -v options increase the verbosity.")
    public void setVerbose(boolean[] verbose) {
        switch (verbose.length) {
            case 1:
                ROOT.setLevel(ch.qos.logback.classic.Level.INFO);
                break;

            case 2:
                ROOT.setLevel(ch.qos.logback.classic.Level.DEBUG);
                break;

            default:
                ROOT.setLevel(ch.qos.logback.classic.Level.TRACE);
                break;
        }
    }

    @Override
    public void run() {
        final CommandLine cmd = this.spec.commandLine();

        cmd.usage(cmd.getErr());
    }


    int run(String... args) {
        return new CommandLine(new BoatTerminal())
            .setExecutionExceptionHandler(this::handleExecutionException)
            .execute(args);
    }

    private int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult)
        throws Exception {
        if (ROOT.isDebugEnabled()) {
            ROOT.debug(commandLine.getCommandName(), ex);
        } else {
            ROOT.error("{}: {}", commandLine.getCommandName(), ex.getMessage());
        }

        return 0;
    }
}
