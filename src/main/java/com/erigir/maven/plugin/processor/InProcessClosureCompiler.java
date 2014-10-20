package com.erigir.maven.plugin.processor;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 Copyright 2014 Christopher Weiss

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
public class InProcessClosureCompiler extends CommandLineRunner {
    private String source;
    private Charset inputCharset = Charset.forName("UTF-8");

    private InProcessClosureCompiler(String source, String[] args, PrintStream out, PrintStream err) {
        super(args, out, err);
        this.source = source;
    }

    protected CompilerOptions createOptions() {
        CompilerOptions options = super.createOptions();
        //addMyCrazyCompilerPassThatOutputsAnExtraFile(options);
        return options;
    }

    public static String compileJavascriptString(Log log,String source) {
        return compileJavascriptString(log, source, new String[0]);

    }

    public static String compileJavascriptString(Log log, String source, String[] args) {
        String rval = null;
        log.info("Running closure compiler...");

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ByteArrayOutputStream bErr = new ByteArrayOutputStream();
            InProcessClosureCompiler runner = new InProcessClosureCompiler(source, args, new PrintStream(bOut), new PrintStream(bErr));
            if (runner.shouldRunCompiler()) {
                try {
                    runner.run();
                } catch (TriedToCallSystemExitException tcse) {
                    log.debug("Google tried to call system exit.  Swallowing");
                }
                rval = new String(bOut.toByteArray());
            } else {
                log.warn("Error running compiler : not ready to run");
            }

        return rval;

    }


    public static void disableSystemExit(Log log) {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null || !DisableSystemExitSecurityManager.class.isInstance(sm)) {
            log.info("Setting system security manager to prevent System.exit");
            System.setSecurityManager(new DisableSystemExitSecurityManager(sm));
        }
    }

    public static void enableSystemExit(Log log) {

        SecurityManager sm = System.getSecurityManager();

        if (sm != null && DisableSystemExitSecurityManager.class.isAssignableFrom(sm.getClass())) {
            log.info("Restoring system security manager ability to call System.exit");
            DisableSystemExitSecurityManager dss = (DisableSystemExitSecurityManager) sm;
            System.setSecurityManager(dss.getWrapped());
        }
        else
        {
            log.info("Couldn't enable security manager (class was "+sm.getClass()+" not DSESM)");
        }
    }


    protected List<SourceFile> createInputs(List<String> files,
                                            boolean allowStdIn) throws FlagUsageException, IOException {
        SourceFile rval = SourceFile.fromCode("test.js", source);//   .fromFile(sourceFile);
        return Arrays.asList(new SourceFile[]{rval});

        //List<SourceFile> inputs = new ArrayList<SourceFile>(1);
        //boolean usingStdin = false;
        /*
    for (String filename : files) {
      if (!"-".equals(filename)) {
        SourceFile newFile = SourceFile.fromFile(filename, inputCharset);
        inputs.add(newFile);
      } else {
        if (!allowStdIn) {
          throw new FlagUsageException("Can't specify stdin.");
        }
        if (usingStdin) {
          throw new FlagUsageException("Can't specify stdin twice.");
        }

        if (!config.outputManifests.isEmpty()) {
          throw new FlagUsageException("Manifest files cannot be generated " +
              "when the input is from stdin.");
        }
        if (!config.outputBundles.isEmpty()) {
          throw new FlagUsageException("Bundle files cannot be generated " +
              "when the input is from stdin.");
        }
        inputs.add(SourceFile.fromInputStream("stdin", System.in));
        usingStdin = true;
      }
    }
          */

        //return inputs;
    }

}
