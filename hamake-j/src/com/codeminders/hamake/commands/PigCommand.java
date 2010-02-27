package com.codeminders.hamake.commands;

import com.codeminders.hamake.Config;
import com.codeminders.hamake.Param;
import com.codeminders.hamake.Utils;
import com.codeminders.hamake.params.PigParam;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.pig.ExecType;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.impl.PigContext;
import org.apache.pig.tools.grunt.Grunt;
import org.apache.pig.tools.parameters.ParameterSubstitutionPreprocessor;
import org.apache.pig.tools.parameters.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public class PigCommand extends BaseCommand {

    private String script;

    public PigCommand() {
    }

    public PigCommand(String script, Collection<Param> parameters) {
        setScript(script);
        setParameters(parameters);
    }

    public int execute(Map<String, Collection> parameters, Map<String, Object> context) {
        DFSClient fsClient = Utils.getFSClient(context);
        Collection<String> args = new ArrayList<String>();

        Collection<Param> scriptParams = getParameters();
        if (scriptParams != null) {
            for (Param p : scriptParams) {
                if (p instanceof PigParam) {
                    PigParam pp = (PigParam) p;
                    Collection<String> values;
                    try {
                        values = p.get(parameters, fsClient);
                    } catch (IOException ex) {
                        System.err.println("Failed to extract parameter values from parameter " +
                                pp.getName() + ": " + ex.getMessage());
                        if (Config.getInstance().test_mode)
                            ex.printStackTrace();
                        return -1000;
                    }
                    if (values.size() != 1) {
                        System.err.println("Multiple values for param are no supported in PIG scripts");
                        return -1000;
                    }
                    args.add(pp.getName() + '=' + values.iterator().next());
                }
            }
        }

        // TODO: Do we need to define some properties?
        Properties pigProps = new Properties();
        PigContext ctx = new PigContext(ExecType.MAPREDUCE, pigProps);

        try {
            // Run, using the provided file as a pig file
            BufferedReader in = new BufferedReader(new FileReader(getScript()));
            // run parameter substitution preprocessor first
            File substFile = File.createTempFile("subst", ".pig");
            BufferedReader pin = preprocessPigScript(in, args, substFile, Config.getInstance().dryrun);
            if (Config.getInstance().dryrun) {
                System.err.println("Substituted pig script is at " + substFile);
                return 0;
            }

            // Set job name based on name of the script
            ctx.getProperties().setProperty(PigContext.JOB_NAME,
                                                   "PigLatin:" + getScript());

            substFile.deleteOnExit();

            Grunt grunt = new Grunt(pin, ctx);
            int results[] = grunt.exec();
            // results:
            // 0: succeeded
            // 1: failed
            return results[1] == 0 ? 0 : -1000;

        } catch (ExecException ex) {
            System.err.println("Failed to execute PIG command " + getScript() + ": " + ex.getMessage());
            if (Config.getInstance().test_mode)
                ex.printStackTrace();
            return -1000;
        } catch (IOException ex) {
            System.err.println("Failed to execute PIG command " + getScript() + ": " + ex.getMessage());
            if (Config.getInstance().test_mode)
                ex.printStackTrace();
            return -1000;
        } catch (ParseException ex) {
            System.err.println("Failed to execute PIG command " + getScript() + ": " + ex.getMessage());
            if (Config.getInstance().test_mode)
                ex.printStackTrace();
            return -1000;
        } catch (Throwable ex) {
            System.err.println("Failed to execute PIG command " + getScript() + ": " + ex.getMessage());
            if (Config.getInstance().test_mode)
                ex.printStackTrace();
            return -1000;
        }
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("script", script).appendSuper(super.toString()).toString();
    }

    protected BufferedReader preprocessPigScript(BufferedReader origPigScript,
                                                 Collection<String> params,
                                                 File scriptFile,
                                                 boolean createFile)
            throws ParseException, IOException {
        ParameterSubstitutionPreprocessor psp = new ParameterSubstitutionPreprocessor(50);
        String[] type1 = new String[1];

        if (createFile) {
            BufferedWriter fw = new BufferedWriter(new FileWriter(scriptFile));
            psp.genSubstitutedFile(origPigScript, fw, params.size() > 0 ? params.toArray(type1) : null,
                    null);
            return new BufferedReader(new FileReader(scriptFile));

        } else {
            StringWriter writer = new StringWriter();
            psp.genSubstitutedFile(origPigScript, writer, params.size() > 0 ? params.toArray(type1) : null,
                    null);
            return new BufferedReader(new StringReader(writer.toString()));
        }
    }

}
