import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;

public class CreateMD5 extends SourceTask {
    private final WorkerExecutor workerExecutor;
    private final DirectoryProperty destinationDirectory;
    private final ConfigurableFileCollection codecClasspath;

    @Inject
    public CreateMD5(WorkerExecutor workerExecutor) {
        super();
        this.workerExecutor = workerExecutor;
        this.destinationDirectory = getProject().getObjects().directoryProperty();
        this.codecClasspath = getProject().getObjects().fileCollection();
    }

    @OutputDirectory
    public DirectoryProperty getDestinationDirectory() {
        return destinationDirectory;
    }

    @InputFiles
    public ConfigurableFileCollection getCodecClasspath() {
        return codecClasspath;
    }

    @TaskAction
    public void createHashes() {

        WorkQueue workQueue = workerExecutor.processIsolation(workerSpec -> {
            workerSpec.getClasspath().from(codecClasspath);
            workerSpec.forkOptions(options -> {
                options.setMaxHeapSize("64m");
            });
        });

        for (File sourceFile : getSource().getFiles()) {
            Provider<RegularFile> md5File = destinationDirectory.file(sourceFile.getName() + ".md5");
            workQueue.submit(GenerateMD5.class, parameters -> {
                parameters.getSourceFile().set(sourceFile);
                parameters.getMD5File().set(md5File);
            });
        }
    }
}