package hu.bme.mit.spaceship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Named;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class SystemTests {

    /**
     * Test the ship using the command line interface.
     *
     * Input commands are provided as a parameter. Expected outputs may be provided in another file.
     * In case the output file does not exist, it is simply ignored and the test is inconclusive.
     */
    @ParameterizedTest
    @MethodSource("provideTestFilePaths")
    void runCommandsFromFile_Success(Path input, Path output) throws IOException {
        // Arrange
        InputStream in = new FileInputStream(input.toFile());
        OutputStream actualOut = new ByteArrayOutputStream();

        // Act
        CommandLineInterface.run(in, actualOut);

        // Assert
        if (!Files.exists(output)) {
            // No output file was provided; test is inconclusive but we
            // still get coverage metrics for the execution
            inconclusive();
        } else {
            String expected = normalizeString(Files.readString(output));
            String actual = normalizeString(actualOut.toString());
            assertEquals(expected, actual);
        }
    }

    private static Stream<Arguments> provideTestFilePaths() {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/input*");

        List<Arguments> args = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get("test-data/"), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    if (matcher.matches(path)) {
                        Path in = path;
                        Path out = Path.of(path.toString().replace("input", "output"));
                        args.add(Arguments.of(Named.of(in.toString(), in),
                                Named.of(out.toString(), out)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Unexpected IO exception thrown by file visitor");
            e.printStackTrace();
        }

        return args.stream();
    }

    /**
     * Utility method to force a test result to be 'inconclusive'.
     */
    private static void inconclusive() {
        Assumptions.assumeTrue(false, "Inconclusive");
    }

    /**
     * Normalize a string.
     * 
     * This function does the following:
     * <ol>
     * <li>remove comment characters (<code>#</code>) and any characters that follow them in the
     * same line
     * <li>replace all remaining consecutive whitespace with single spaces
     * <li>strip all leading and trailing whitespace
     * </ol>
     */
    private static String normalizeString(String s) {
        return s.replaceAll("#.*", "").replaceAll("\\s+", " ").strip();
    }
}
