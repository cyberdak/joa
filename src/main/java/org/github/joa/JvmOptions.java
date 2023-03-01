/**********************************************************************************************************************
 * JVM Options Analyzer                                                                                               *
 *                                                                                                                    *
 * Copyright (c) 2022-2023 Mike Millson                                                                                    *
 *                                                                                                                    *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License       *
 * v. 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0 which is    *
 * available at https://www.apache.org/licenses/LICENSE-2.0.                                                          *
 *                                                                                                                    *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0                                                                     *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Mike Millson - initial API and implementation                                                                   *
 *********************************************************************************************************************/
package org.github.joa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.github.joa.domain.Bit;
import org.github.joa.domain.GarbageCollector;
import org.github.joa.domain.JvmContext;
import org.github.joa.domain.Os;
import org.github.joa.util.Analysis;
import org.github.joa.util.Constants;
import org.github.joa.util.JdkMath;
import org.github.joa.util.JdkRegEx;
import org.github.joa.util.JdkUtil;

/**
 * <p>
 * JVM options. Null indicates the option is not explicitly set.
 * </p>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 */
public class JvmOptions {

    /**
     * The option to explicitly set the number of cpu/cores and override any <code>useContainerSupport</code> settings.
     * Added in JDK8 u191.
     * 
     * For example:
     * 
     * <pre>
     * -XX:ActiveProcessorCount=2
     * </pre>
     */
    private String activeProcessorCount;

    /**
     * The percentage weight to give to recent gc stats (vs historic) for ergonomic calculations. For example:
     * 
     * <pre>
     * -XX:AdaptiveSizePolicyWeight=90
     * </pre>
     */
    private String adaptiveSizePolicyWeight;

    /**
     * Module exports. For example:
     * 
     * <pre>
     * -add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
     * </pre>
     */
    private ArrayList<String> addExports = new ArrayList<String>();

    /**
     * Runtime modules. For example:
     * 
     * <pre>
     * --add-modules=ALL-SYSTEM
     * </pre>
     */
    private ArrayList<String> addModules = new ArrayList<String>();

    /**
     * Used to allow deep reflection to access nonpublic members. For example:
     * 
     * <pre>
     * --add-opens=java.base/java.security=ALL-UNNAMED
     * </pre>
     */
    private ArrayList<String> addOpens = new ArrayList<String>();

    /**
     * JVM option to load a native agent by library name.
     * 
     * For example:
     * 
     * -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n
     * 
     * -agentlib:am_sun_16=/opt/jbossapps/tgkcpmj02/itcam/runtime/jboss7.tagp3aps3.tgkcpmj02/dc.env.properties
     */
    private ArrayList<String> agentlib = new ArrayList<String>();

    /**
     * JVM option to load a native agent by full path.
     * 
     * For example:
     * 
     * -agentpath:/path/to/agent.so
     */
    private ArrayList<String> agentpath = new ArrayList<String>();

    /**
     * Option to enable/disable aggressive heap management for long running, memory intensive processes.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+AggressiveHeap
     * </pre>
     */
    private String aggressiveHeap;

    /**
     * Option to enable/disable various experimental performance optimizations that have varied over time. Disabled by
     * default and deprecated in JDK11.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+AggressiveOpts
     * </pre>
     */
    private String aggressiveOpts;

    /**
     * Option to enable/disable to touch all pages of the Java heap on startup to avoid the performance penalty at
     * runtime.
     * 
     * <pre>
     *-XX:+AlwaysPreTouch
     * </pre>
     */
    private String alwaysPreTouch;

    /**
     * Analysis.
     */
    private List<Analysis> analysis;

    /**
     * The upper limit of Integers to cache. The lower limit is fixed at -128, and the upper limit defaults to 127. The
     * following option would cache Integers between -128 and 1000:
     * 
     * <pre>
     * -XX:AutoBoxCacheMax=1000
     * </pre>
     */
    private String autoBoxCacheMax;

    /**
     * Option to enable/disable background compilation of bytecode. For example:
     * 
     * <pre>
     * -XX:-BackgroundCompilation
     * </pre>
     */
    private String backgroundCompilation;

    /**
     * Option to disable background compilation of bytecode. For example:
     * 
     * <pre>
     * -Xbatch
     * </pre>
     */
    private boolean batch = false;

    /**
     * JVM options for bootstrap classes and resources. Multiple options are cumulative, not overriding.
     * 
     * For example:
     * 
     * -Xbootclasspath/p:/path/to/some.jar
     */
    private ArrayList<String> bootclasspath = new ArrayList<String>();

    /**
     * Option to enable additional validation checks on the arguments passed to JNI functions. Logging is sent to
     * standard out.
     * 
     * For example:
     * 
     * <pre>
     * -Xcheck:jni
     * </pre>
     */
    private boolean checkJni = false;

    /**
     * The number of compiler threads. For example:
     * 
     * <pre>
     * -XX:CICompilerCount=2
     * </pre>
     */
    private String ciCompilerCount;

    /**
     * The option to set the classpath. For example:
     * 
     * <pre>
     * -classpath /path/to/tomcat/bin/bootstrap.jar:/path/to/tomcat/bin/tomcat-juli.jar:/path/to/java/ant.jar:
     * /path/to/java/ant-launcher.jar:/path/to/java/lib/tools.jar
     * </pre>
     */
    private String classpath;

    /**
     * The option to enable/disable class unloading during gc. For example:
     * 
     * <pre>
     * -XX:-ClassUnloading
     * </pre>
     */
    private String classUnloading;

    /**
     * Option to enable the client JIT compiler, a separate Java binary, optimized for fast startup and small footprint
     * (e.g. interactive GUI applications). Only available on 32-bit JDKS. Both the client and server binaries are
     * included in 32-bit JDKS. Only the client binary is included in 32-bit JREs. For example:
     * 
     * <pre>
     * -client
     * </pre>
     */
    private boolean client = false;

    /**
     * The option to enable/disable the CMS collector to collect perm/metaspace. For example:
     * 
     * <pre>
     * -XX:+CMSClassUnloadingEnabled
     * </pre>
     */
    private String cmsClassUnloadingEnabled;

    /**
     * Option to enable/disable always recording CMS parallel initial mark/remark eden chunks. For example:
     * 
     * <pre>
     * -XX:+CMSEdenChunksRecordAlways
     * </pre>
     */
    private String cmsEdenChunksRecordAlways;

    /**
     * The option to enable/disable the CMS collector running in incremental mode.
     * 
     * In incremental mode, the CMS collector does not hold the processor(s) for the entire long concurrent phases but
     * periodically stops them and yields the processor back to other threads in the application. It divides the work to
     * be done in concurrent phases into small chunks called duty cycles and schedules them between minor collections.
     * This is very useful for applications that need low pause times and are run on machines with a small number of
     * processors.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+CMSIncrementalMode
     * </pre>
     */
    private String cmsIncrementalMode;

    /**
     * The option for setting starting the concurrent collection NN% sooner than the calculated time. For example:
     * 
     * <pre>
     * -XX:CMSIncrementalSafetyFactor=20
     * </pre>
     */
    private String cmsIncrementalSafetyFactor;

    /**
     * The option for setting CMS initiating occupancy fraction, the tenured generation occupancy percentage that
     * triggers a concurrent collection. For example:
     * 
     * <pre>
     * -XX:CMSInitiatingOccupancyFraction=75
     * </pre>
     */
    private String cmsInitiatingOccupancyFraction;

    /**
     * Option to enable/disable CMS multi-threaded initial mark. For example:
     * 
     * <pre>
     * -XX:-CMSParallelInitialMarkEnabled
     * </pre>
     */
    private String cmsParallelInitialMarkEnabled;

    /**
     * Option to enable/disable CMS multi-threaded initial remark. For example:
     * 
     * <pre>
     * -XX:-CMSParallelRemarkEnabled
     * </pre>
     */
    private String cmsParallelRemarkEnabled;

    /**
     * Option to enable a young collection before the CMS remark phase, pruning the young generation to minimize remark
     * pause times.
     * 
     * 
     * <pre>
     *-XX:+CMSScavengeBeforeRemark
     * </pre>
     */
    private String cmsScavengeBeforeRemark;

    /**
     * Option for setting how long the CMS collector will wait for a young space collection before starting initial
     * marking. For example:
     * 
     * <pre>
     * -XX:CMSWaitDuration=10000
     * </pre>
     */
    private String cmsWaitDuration;

    /**
     * The option to enable compilation of bytecode on first invocation. For example:
     * 
     * <pre>
     * -Xcomp
     * </pre>
     */
    private boolean comp = false;

    /**
     * The option for specifying a command for the Just in Time (JIT) compiler to execute on a method. For example:
     * 
     * <pre>
     * -XX:CompileCommand=exclude,com/example/MyClass
     * </pre>
     */
    private String compileCommand;

    /**
     * The option for specifying a compile command file for the Just in Time (JIT) compiler. For example:
     * 
     * <pre>
     * -XX:CompileCommandFile=/etc/cassandra/conf/hotspot_compiler
     * </pre>
     */
    private String compileCommandFile;

    /**
     * The option for setting the number of method executions before a method is compiled from bytecode to native code
     * by the Just in Time (JIT) compiler with "standard compilation" (when tiered compilation is disabled). Ignored
     * when tiered compilation is enabled.
     * 
     * Default is 10,000. Setting -XX:CompileThreshold=1 forces compiling at first execution.
     * 
     * For example:
     * 
     * <pre>
     * -XX:CompileThreshold=500
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String compileThreshold;

    /**
     * The option for setting the virtual (reserved) size of the compressed class space (a single area). For example:
     * 
     * <pre>
     * -XX:CompressedClassSpaceSize=768m
     * </pre>
     */
    private String compressedClassSpaceSize;

    /**
     * The number of concurrent GC threads. For example:
     * 
     * <pre>
     * -XX:ConcGCThreads=18
     * </pre>
     */
    private String concGcThreads;

    /**
     * Mysteriously named, undocumented option that sets the following 4 options:
     * 
     * <pre>
     * -XX:-BackgroundCompilation
     * -XX:DeferThrSuspendLoopCount=1
     * -XX:NewSizeThreadIncrease=16384
     * -XX:-UseTLAB
     * </pre>
     * 
     * It has no known, valid uses and was removed in JDK12:
     * 
     * https://bugs.openjdk.org/browse/JDK-8213767
     * 
     * For example:
     * 
     * <pre>
     * -Xconcurrentio
     * </pre>
     */
    private boolean concurrentio;

    /**
     * Option to enable/disable the jvm process exiting (and producing a fatal error log and core as applicable) when
     * out of memory occurs.
     * 
     * <pre>
     *-XX:+CrashOnOutOfMemoryError
     * </pre>
     */
    private String crashOnOutOfMemoryError;

    /**
     * The option for specifying 64-bit. Removed in JDK11.
     * 
     * <pre>
     * -d64
     * </pre>
     */
    private boolean d64 = false;

    /**
     * Option to enable debugging using the Java Virtual Machine Debug Interface (JVMDI). JVMDI has been removed, so
     * this option does nothing. For example:
     * 
     * <pre>
     * -Xdebug
     * </pre>
     */
    private boolean debug = false;

    /**
     * Diagnostic option (requires <code>-XX:+UnlockDiagnosticVMOptions</code>) to enable/disable the compiler
     * generating metadata for code not at safe points to improve the accuracy of Java Flight Recorder (JFR) Method
     * Profiler.
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints
     * </pre>
     */
    private String debugNonSafepoints;

    /**
     * Diagnostic JVM options (require <code>-XX:+UnlockDiagnosticVMOptions</code>).
     */
    private ArrayList<String> diagnostic = new ArrayList<String>();

    /**
     * The option to disable the creation of the AttachListener socket file (/tmp/.java_pid<pid>) used by
     * jcmd/jmap/jstack to communicate with the JVM. Created the first time jcmd/jmap/jstack is run.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+DisableAttachMechanism
     * </pre>
     */
    private String disableAttachMechanism;

    /**
     * The option to enable/disable explicit garbage collection. For example:
     * 
     * <pre>
     * -XX:+DisableExplicitGC
     * </pre>
     */
    private String disableExplicitGc;

    /**
     * Option to enable/disable Just In Time (JIT) compiler optimization for objects created and referenced by a single
     * thread within the scope of a method:
     * 
     * 1) Objects are not created, and their fields are treated as local variables and allocated on the method stack or
     * in cpu registers. This effectively moves the allocation from the heap to the stack, which is much faster.
     * 
     * 2) Locking and memory synchronization is removed, reducing overhead.
     * 
     * Enabled by default in JDK 1.6+.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+DoEscapeAnalysis
     * </pre>
     */
    private String doEscapeAnalysis;

    /**
     * The option to enable/disable the compiler optimization to eliminate locks if the monitor is not reachable from
     * other threads. Enabled by default in JDK 1.8+.
     * 
     * <pre>
     * -XX:+EliminateLocks
     * </pre>
     */
    private String eliminateLocks;

    /**
     * The option to specify the location where a fatal error log will be written. For example:
     * 
     * <pre>
     *  -XX:ErrorFile=/mydir/hs_err_pid%p.log
     * </pre>
     */
    private String errorFile;

    /**
     * Option to enable/disable the JVM process exiting on OutOfMemoryError. For example:
     * 
     * <pre>
     * -XX:+ExitOnOutOfMemoryError
     * </pre>
     */
    private String exitOnOutOfMemoryError;

    /**
     * Experimental JVM options (require <code>-XX:+UnlockExperimentalVMOptions</code>).
     */
    private ArrayList<String> experimental = new ArrayList<String>();

    /**
     * The option to enable/disable explicit garbage collection to be handled concurrently by the CMS and G1 collectors.
     * For example:
     * 
     * <pre>
     * -XX:+ExplicitGCInvokesConcurrent
     * </pre>
     */
    private String explicitGCInvokesConcurrent;

    /**
     * The option to enable/disable explicit garbage collection to be handled concurrently by the CMS and G1 collectors
     * and classes unloaded. For example:
     * 
     * <pre>
     * -XX:-ExplicitGCInvokesConcurrentAndUnloadsClasses
     * </pre>
     */
    private String explicitGCInvokesConcurrentAndUnloadsClasses;

    /**
     * Option to enable/disable outputting additional information in the fatal error logs. For example:
     * 
     * <pre>
     * -XX:+ExtensiveErrorReports
     * </pre>
     */
    private String extensiveErrorReports;

    /**
     * The option for starting Java Flight Recorder (JFR). For example:
     * 
     * <pre>
     * -XX:FlightRecorderOptions=stackdepth=256
     * </pre>
     */
    private String flightRecorderOptions;

    /**
     * The number of G1 concurrent refinement threads. For example:
     * 
     * <pre>
     * -XX:G1ConcRefinementThreads=4
     * </pre>
     */
    private String g1ConcRefinementThreads;

    /**
     * The option to set the size of the G1 region. It has to be a power of 2 between 1m - 32m.
     * 
     * It is calculated as MaxHeapSize/2048 rounded down to the power of 2:
     * 
     * MaxHeapSize < 4g: 1m
     * 
     * 4g <= MaxHeapSize < 8g: 2m
     * 
     * 8g <= MaxHeapSize < 16g: 4m
     * 
     * 16g <= MaxHeapSize < 32g: 8m
     * 
     * 32g <= MaxHeapSize < 64g: 16m
     * 
     * MaxHeapSize >= 64g: 32m
     * 
     * For example:
     * 
     * <pre>
     * -XX:G1HeapRegionSize=4m
     * </pre>
     */
    private String g1HeapRegionSize;

    /**
     * The option for setting the G1 heap waste percentage (default 10). A mixed GC cycle will not be initiated if the
     * reclaimable percentage is less.
     * 
     * For example:
     * 
     * <pre>
     * -XX:G1HeapWastePercent=5
     * </pre>
     */
    private String g1HeapWastePercent;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) that sets the percentage of the heap
     * to use as the maximum new generation size (default 60%). Replaces <code>-XX:DefaultMaxNewGenPercent</code>.
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:G1MaxNewSizePercent=30
     * </pre>
     */
    private String g1MaxNewSizePercent;

    /**
     * Option to set the target number of mixed garbage collections (default 8) after a marking cycle to collect old
     * regions. For example:
     * 
     * <pre>
     * -XX:G1MixedGCCountTarget=4
     * </pre>
     */
    private String g1MixedGCCountTarget;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) for setting the occupancy threshold
     * for a region to be considered as a candidate region for a G1_CLEANUP collection.
     * 
     * Prior to JDK8 update 40 the default values for G1HeapWastePercent (10) and G1MixedGCLiveThresholdPercent (65) are
     * generally very bad, and the standard recommended configuration is to use this experimental option:
     * -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85 -XX:G1HeapWastePercent=5.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85
     * </pre>
     */
    private String g1MixedGCLiveThresholdPercent;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) to set the percentage of the heap to
     * use as the initial young generation size.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=1
     * </pre>
     */
    private String g1NewSizePercent;

    /**
     * The G1 collector option for setting the percentage of heap space that should be kept in reserve (not used) to
     * minimize the probability of promotion failures. It is a safety net in hopes of avoiding a G1 full collection.
     * When increasing, it is necessary to increase the total heap size by an equivalent percentage to keep the amount
     * of allocation space constant.
     * 
     * For example:
     * 
     * <pre>
     * -XX:G1ReservePercent=10 (default)
     * </pre>
     */
    private String g1ReservePercent;

    /**
     * Option to enable/disable output of summarized remembered set processing info. For example:
     * 
     * <pre>
     * -XX:+G1SummarizeRSetStats
     * </pre>
     */
    private String g1SummarizeRSetStats;

    /**
     * The option for setting the # of GCs to output update buffer processing info (0 = disabled). For example:
     * 
     * <pre>
     * -XX:G1SummarizeRSetStatsPeriod=1
     * </pre>
     */
    private String g1SummarizeRSetStatsPeriod;

    /**
     * Diagnostic option (requires <code>-XX:+UnlockDiagnosticVMOptions</code>) to specify the number of times a thread
     * should retry its allocation when blocked by the GC locker.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions -XX:GCLockerRetryAllocationCount=4
     * </pre>
     */
    private String gcLockerRetryAllocationCount;

    /**
     * Size of gc log file that triggers rotation. For example:
     * 
     * <pre>
     * -XX:GCLogFileSize=3M
     * </pre>
     */
    private String gcLogFileSize;

    /**
     * The ratio of GC time to application time. For example:
     * 
     * <pre>
     * -XX:GCTimeRatio=4
     * </pre>
     */
    private String gcTimeRatio;

    /**
     * Diagnostic option (requires <code>-XX:+UnlockDiagnosticVMOptions</code>) to set a minimal safepoint interval
     * (ms). For example:
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=90000000
     * </pre>
     */
    private String guaranteedSafepointInterval;

    /**
     * The option for setting the Java heap base memory address (where the heap allocation begins). For example:
     * 
     * <pre>
     * -XX:HeapBaseMinAddress=12g
     * </pre>
     */
    private String heapBaseMinAddress;

    /**
     * The option to write out a heap dump when OutOfMemoryError. For example:
     * 
     * <pre>
     * -XX:+HeapDumpOnOutOfMemoryError
     * </pre>
     */
    private String heapDumpOnOutOfMemoryError;

    /**
     * The option to specify the location where a heap dump will be written on OutOfMemoryError. For example:
     * 
     * <pre>
     *  -XX:HeapDumpPath=/mydir/
     * </pre>
     */
    private String heapDumpPath;

    /**
     * Option to enable/disable checking for unrecognized options passed to the JVM. For example:
     * 
     * <pre>
     * -XX:+IgnoreUnrecognizedVMOptions
     * </pre>
     */
    private String ignoreUnrecognizedVmOptions;

    /**
     * The initial boot classloader metaspace size (default 4M). For example:
     * 
     * <pre>
     * -XX:InitialBootClassLoaderMetaspaceSize=8M
     * </pre>
     */
    private String initialBootClassLoaderMetaspaceSize;

    /**
     * Initial heap space size. Specified with the <code>-Xms</code> or <code>-XX:InitialHeapSize</code> option. For
     * example:
     * 
     * <pre>
     * -Xms1024m
     * -XX:InitialHeapSize=257839744
     * </pre>
     */
    private String initialHeapSize;

    /**
     * The heap occupancy threshold to start a concurrent GC cycle (G1 marking). Default is 45%. Lower it to start
     * marking earlier to avoid marking not finishing before heap fills up (analogous to CMS concurrent mode failure). A
     * value of 0 results in constant GC cycles.
     * 
     * For example:
     * 
     * <pre>
     * -XX:InitiatingHeapOccupancyPercent=40
     * </pre>
     */
    private String initiatingHeapOccupancyPercent;

    /**
     * Instrumentation (byte code manipulation).
     * 
     * For example:
     * 
     * -javaagent:/path/to/appdynamics/javaagent.jar
     */
    private ArrayList<String> javaagent = new ArrayList<String>();

    /**
     * JVM context information.
     */
    private JvmContext jvmContext;

    /**
     * The option for setting the virtual (reserved) size of the compressed class space (a single area). Only has
     * meaning on Solaris. Other OS like Linux use the page size the kernel is set to support
     * (<code>Hugepagesize</code>). On Windows it cannot be set (like in Linux) and is fixed at 2MB.
     * 
     * For example:
     * 
     * <pre>
     * -XX:LargePageSizeInBytes=4m
     * </pre>
     */
    private String largePageSizeInBytes;

    /**
     * Option to specify gc logging options in JDK11+. For example:
     * 
     * <p>
     * 1) Single option:
     * </p>
     * 
     * <pre>
     * -Xlog:gc*,gc+age=trace,safepoint:file=/path/to/gc.log:utctime,pid,tags:filecount=4,filesize=64m
     * </pre>
     * 
     * <p>
     * 2) Multiple options:
     * </p>
     * 
     * <pre>
     * -Xlog:gc*=debug:file=/path/to/gc-%t.log:time,pid,tid,level,tags:filesize=1G 
     * -Xlog:all=info,exceptions=warning,gc*=off:file=/path/to/vm-%t.log:time,pid,tid,level,tags:filesize=100M
     * </pre>
     */
    private ArrayList<String> log = new ArrayList<String>();

    /**
     * The option to specify the location where safepoint logging will be written. For example:
     * 
     * <pre>
     *  -XX:LogFile=/path/to/vm.log
     * </pre>
     */
    private String logFile;

    /**
     * Option to specify gc log location in JDK8. For example:
     * 
     * <pre>
     * -Xloggc:/path/to/EAP-7.1.0/standalone/log/gc.log
     * </pre>
     */
    private String loggc;

    /**
     * Diagnostic option (requires <code>-XX:+UnlockDiagnosticVMOptions</code>) to enable/disable vm logging for
     * safepoint analysis. For example:
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput
     * </pre>
     */
    private String logVmOutput;

    /**
     * Option to set the number of iterations in the inner strip mined loop (1000 default). For example:
     * 
     * <pre>
     * -XX:LoopStripMiningIter=1000
     * </pre>
     */
    private String loopStripMiningIter;

    /**
     * Option to enable/disable JMX. For example:
     * 
     * <pre>
     * -XX:+ManagementServer.
     * </pre>
     */
    private String managementServer;

    /**
     * The option for setting the marking stack initial size. For example:
     * 
     * <pre>
     * -XX:MarkStackSize=4194304
     * </pre>
     */
    private String markStackSize;

    /**
     * The option for setting the marking stack max size. For example:
     * 
     * <pre>
     * -XX:MarkStackSizeMax=2147483646
     * </pre>
     */
    private String markStackSizeMax;

    /**
     * Maximum direct memory. Attempting to allocate direct memory that would cause the limit to be exceeded causes a
     * full GC to initiate reference processing and release of unreferenced buffers.
     * 
     * For example:
     * 
     * <pre>
     * -XX:MaxDirectMemorySize=8g
     * </pre>
     */
    private String maxDirectMemorySize;

    /**
     * The option to enable/disable the maximum file descriptors (solaris only). For example:
     * 
     * <pre>
     * -XX:+MaxFDLimit
     * </pre>
     */
    private String maxFdLimit;

    /**
     * The option for setting the maximum gc pause time ergonomic option. For example:
     * 
     * <pre>
     * -XX:MaxGCPauseMillis=500
     * </pre>
     */
    private String maxGcPauseMillis;

    /**
     * The maximum percentage of free space to avoid shrinking the heap size. For example:
     * 
     * <pre>
     * -XX:MaxHeapFreeRatio=20
     * </pre>
     */
    private String maxHeapFreeRatio;

    /**
     * Maximum heap space. Specified with the <code>-Xmx</code> or <code>-XX:MaxHeapSize</code> option. For example:
     * 
     * <pre>
     * -Xmx1024m
     * -XX:MaxHeapSize=1234567890
     * </pre>
     */
    private String maxHeapSize;

    /**
     * Maximum number of nested calls to inline.
     * 
     * For example:
     * 
     * <pre>
     * -XX:MaxInlineLevel=15
     * </pre>
     */
    private String maxInlineLevel;

    /**
     * The option for setting the number of lines in stack trace output. For example:
     * 
     * <pre>
     * jvmOptions
     * -XX:MaxJavaStackTraceDepth=50000
     * </pre>
     */
    private String maxJavaStackTraceDepth;

    /**
     * Equivalent to {@link #reservedCodeCacheSize}.
     * 
     * For example:
     * 
     * <pre>
     * -Xmaxjitcodesize1024m
     * </pre>
     */
    private String maxjitcodesize;

    /**
     * Maximum committed metaspace (class metadata + compressed class space). For example:
     * 
     * <pre>
     * -XX:MaxMetaspaceSize=2048m
     * </pre>
     */
    private String maxMetaspaceSize;

    /**
     * Maximum new generation size. The following forumula is used to determine new generation size:
     * 
     * min(MaxNewSize, max(NewSize, heap/(NewRatio+1)))
     * 
     * For example:
     * 
     * <pre>
     * -XX:MaxNewSize=512m
     * </pre>
     */
    private String maxNewSize;

    /**
     * Maximum permanent generation size. In JDK8 the permanent generation space was replaced by the metaspace, so this
     * option will be ignored. For example:
     * 
     * <pre>
     * -XX:MaxPermSize=256m
     * </pre>
     */
    private String maxPermSize;

    /**
     * The option for setting the maximum tenuring threshold option (the number of times objects surviving a young
     * collection are copied to a survivor space). For example:
     * 
     * <pre>
     * -XX:MaxTenuringThreshold=0
     * </pre>
     */
    private String maxTenuringThreshold;

    /**
     * The allocated class metadata space size that will trigger a garbage collection when it is exceeded for the first
     * time. The JVM may choose a new threshold after the initial threshold is exceeded. The default size is platform
     * dependent. For example:
     * 
     * <pre>
     * -XX:MetaspaceSize=1024M
     * </pre>
     */
    private String metaspaceSize;

    /**
     * The minimum amount to resize the heap space in bytes. For example:
     * 
     * <pre>
     * --XX:MinHeapDeltaBytes=123456
     * </pre>
     */
    private String minHeapDeltaBytes;

    /**
     * The minimum percentage of free space to avoid expanding the heap size. For example:
     * 
     * <pre>
     * -XX:MinHeapFreeRatio=10
     * </pre>
     */
    private String minHeapFreeRatio;

    /**
     * The option to enable native memory tracking. For example:
     * 
     * <pre>
     *  -XX:NativeMemoryTracking=detail
     * </pre>
     */
    private String nativeMemoryTracking;

    /**
     * Option to set the ratio of old/new generation sizes.
     * 
     * For example:
     * 
     * <pre>
     * -XX:NewRatio=3
     * </pre>
     */
    private String newRatio;

    /**
     * Initial young generation size. Specified with either the <code>-XX:NewSize</code> or <code>-Xmn</code> option.
     * The following forumula is used to determine new generation size:
     * 
     * min(MaxNewSize, max(NewSize, heap/(NewRatio+1)))
     * 
     * For example:
     * 
     * <pre>
     * -XX:NewSize=1g
     * </pre>
     * 
     * <pre>
     * -Xmn1g
     * </pre>
     */
    private String newSize;

    /**
     * Option to disable the garbage collection of classes. For example:
     * 
     * <pre>
     * -Xnoclassgc
     * </pre>
     */
    private boolean noclassgc = false;

    /**
     * Option to disable class verification on JVM startup. For example:
     * 
     * <pre>
     * -noverify
     * </pre>
     */
    private boolean noverify = false;

    /**
     * Option to specify the number of gc log files to keep when rotation is enabled. For example:
     * 
     * <pre>
     * -XX:NumberOfGCLogFiles=5
     * </pre>
     */
    private String numberOfGcLogFiles;

    /**
     * Option to set the size of the old generation Promotion Local Allocation Buffers (PLABs) in heap words (4 bytes in
     * 32 bit JVM and 64 bit JVM with compressed pointers enabled, 8 bytes otherwise).
     * 
     * For example:
     * 
     * <pre>
     * -XX:OldPLABSize=16
     * </pre>
     */
    private String oldPlabSize;

    /**
     * The default (initial) size of the old (tenured) generation. For example:
     * 
     * <pre>
     * -XX:OldSize=2913992704
     * </pre>
     */
    private String oldSize;

    /**
     * Option to enable/disable the use of preallocated exceptions, an optimization when an exception is thrown many
     * times the JVM stops including the stack trace. For example:
     * 
     * <pre>
     * -XX:-OmitStackTraceInFastThrow
     * </pre>
     */
    private String omitStackTraceInFastThrow;

    /**
     * The option to run a command or script when an irrecoverable error happens. For example:
     * 
     * <pre>
     * -XX:OnError=gcore %p
     * </pre>
     */
    private String onError;

    /**
     * The option to run a command or script when the first OutOfMemoryError happens. For example:
     * 
     * <pre>
     * -XX:OnOutOfMemoryError="pmap %p"
     * </pre>
     */
    private String onOutOfMemoryError;

    /**
     * Option to enable/disable optimizing String concatenation operations. For example:
     * 
     * <pre>
     * s
     * -XX:-OptimizeStringConcat
     * </pre>
     */
    private String optimizeStringConcat;

    /**
     * Map of jvm options.
     */
    private Map<String, ArrayList<String>> options = new HashMap<String, ArrayList<String>>();

    /**
     * Overconstrained JVM options (rare/no use real use cases, so it's likely better to remove them).
     */
    private ArrayList<String> overconstrained = new ArrayList<String>();

    /**
     * The number of parallel gc threads. For example:
     * 
     * <pre>
     * -XX:ParallelGCThreads=4
     * </pre>
     */
    private String parallelGcThreads;

    /**
     * Option to enable/disable multi-threaded reference processing.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+ParallelRefProcEnabled
     * </pre>
     */
    private String parallelRefProcEnabled;

    /**
     * Option to enable/disable the JVM outputting statistics to the hsperfdata file. For example:
     * 
     * <pre>
     * -XX:+PerfDisableSharedMem
     * </pre>
     */
    private String perfDisableSharedMem;

    /**
     * Option to set the maximum number of recompiles (default 400) before stayaing in the interpreter. For example:
     * 
     * <pre>
     * -XX:PerMethodRecompilationCutoff=10000
     * </pre>
     */
    private String perMethodRecompilationCutoff;

    /**
     * Initial permanent generation size. In JDK8 the permanent generation space was replaced by the metaspace, so this
     * option is being ignored. For example:
     * 
     * <pre>
     * -XX:PermSize=128m
     * </pre>
     */
    private String permSize;

    /**
     * The option enable/disable Adaptive Resize Policy output. For example:
     * 
     * <pre>
     * -XX:+PrintAdaptiveSizePolicy
     * </pre>
     */
    private String printAdaptiveSizePolicy;

    /**
     * The option to enable/disable outputting a class histogram in the gc logging when a thread dump is taken. For
     * example:
     * 
     * <pre>
     * -XX:+PrintClassHistogram
     * </pre>
     */
    private String printClassHistogram;

    /**
     * The option to enable/disable outputting a class histogram in the gc logging after every full gc. For example:
     * 
     * <pre>
     * -XX:+PrintClassHistogramAfterFullGC
     * </pre>
     */
    private String printClassHistogramAfterFullGc;

    /**
     * The option to enable/disable outputting a class histogram in the gc logging before every full gc. For example:
     * 
     * <pre>
     * -XX:+PrintClassHistogramBeforeFullGC
     * </pre>
     */
    private String printClassHistogramBeforeFullGc;

    /**
     * The option to enable/disable outputting code cache memory usage when the JVM exits. For example:
     * 
     * <pre>
     * -XX:+PrintCodeCache
     * </pre>
     */
    private String printCodeCache;

    /**
     * The option to enable/disable outputting JVM command line options to standard out on JVM startup. For example:
     * 
     * <pre>
     * -XX:+PrintCommandLineFlags
     * </pre>
     */
    private String printCommandLineFlags;

    /**
     * The option to enable/disable outputting every JVM option and value to standard out on JVM startup. For example:
     * 
     * <pre>
     * -XX:+PrintFlagsFinalparallelRefProcEnabled
     * </pre>
     */
    private String printFlagsFinal;
    /**
     * Option to enable printing CMS Free List Space statistics in gc logging. For example:
     * 
     * <pre>
     * -XX:PrintFLSStatistics=1
     * </pre>
     */
    private String printFLSStatistics;

    /**
     * Option to enable/disable displaying detailed information about each gc event. Equivalent to
     * <code>-verbose:gc</code>. For example:
     * 
     * <pre>
     * -XX:+PrintGC
     * </pre>
     */
    private String printGc;

    /**
     * The option to enable/disable printing application concurrent time in the gc logging. For example:
     * 
     * <pre>
     * -XX:+PrintGCApplicationConcurrentTime
     * </pre>
     */
    private String printGcApplicationConcurrentTime;

    /**
     * Option to enable/disable outputting application stopped time in gc logging. Deprecated in JDK9. For example:
     * 
     * <pre>
     * -XX:+PrintGCApplicationStoppedTime
     * </pre>
     */
    private String printGcApplicationStoppedTime;

    /**
     * Option to enable/disable printing trigger information. Deprecated in JDK9, removed in JDK11. For example:
     * 
     * <pre>
     * -XX:+PrintGCCause
     * </pre>
     */
    private String printGcCause;

    /**
     * Option to enable/disable gc logging datestamps. Deprecated in JDK9. For example:
     * 
     * <pre>
     * -XX:+PrintGCDateStamps
     * </pre>
     */
    private String printGcDateStamps;

    /**
     * Option to enable/disable printing gc details. Deprecated in JDK9. For example:
     * 
     * <pre>
     * -XX:+PrintGCDetails
     * </pre>
     */
    private String printGcDetails;

    /**
     * Option to enable/disable printing task timestamp for each GC thread. For example:
     * 
     * <pre>
     * -XX:+PrintGCTaskTimeStamps
     * </pre>
     */
    private String printGcTaskTimeStamps;

    /**
     * Option to enable/disable printing gc timestamps.
     * 
     * <pre>
     * -XX:+PrintGCTimeStamps
     * </pre>
     */
    private String printGcTimeStamps;

    /**
     * Option to enable/disable printing additional heap information in gc logging.
     * 
     * <pre>
     * -XX:+PrintHeapAtGC
     * </pre>
     */
    private String printHeapAtGc;

    /**
     * Option to enable/disable printing promotion failure information. For example:
     * 
     * <pre>
     * -XX:+PrintPromotionFailure
     * </pre>
     */
    private String printPromotionFailure;

    /**
     * The option to enable/disable outputting times for reference processing (weak, soft, JNI) in gc logging. For
     * example:
     * 
     * <pre>
     * -XX:+PrintReferenceGC
     * </pre>
     */
    private String printReferenceGc;

    /**
     * Diagnostic option (requires </code>-XX:+UnlockDiagnosticVMOptions</code>) to enable/disable printing safepoint
     * information. For example:
     * 
     * <pre>
     *-XX:+UnlockDiagnosticVMOptions -XX:+PrintSafepointStatistics
     * </pre>
     */
    private String printSafepointStatistics;

    /**
     * The option to enable/disable outputting string deduplication statistics in gc logging. For example:
     * 
     * <pre>
     * -XX:+PrintStringDeduplicationStatistics
     * </pre>
     */
    private String printStringDeduplicationStatistics;

    /**
     * The option to enable/disable logging string pool statistics. For example:
     * 
     * <pre>
     * -XX:+PrintStringTableStatistics
     * </pre>
     */
    private String printStringTableStatistics;

    /**
     * Option to enable/disable printing tenuring information in gc logging.
     * 
     * <pre>
     * -XX:+PrintTenuringDistribution
     * </pre>
     */
    private String printTenuringDistribution;

    /**
     * Code cache size (default 240m), where the JVM stores the assembly language instructions of compiled code.
     * 
     * It's only necessary to set the max size, not min and max, for the following reasons:
     * 
     * 1) Memory is not allocated until needed, so setting a large code cache size only impacts reserved (virtual)
     * memory, not allocated (physical) memory.
     * 
     * 2) Resizing the code cache is done in the background and does not affect performance.
     * 
     * For example:
     * 
     * <pre>
     * -XX:ReservedCodeCacheSize=256m
     * </pre>
     */
    private String reservedCodeCacheSize;

    /**
     * Option to enable/disable dynamic resizing of the Promotion Local Allocation Buffers (PLABs). Each GC thread has
     * two PLABs, one for the survivor space and one for the old space.
     * 
     * Can cause performance issues with the G1 collector for some loads; therefore, is sometimes disabled to decrease
     * G1 GC pause time.
     * 
     * For example:
     * 
     * <pre>
     * -XX:-ResizePLAB
     * </pre>
     */
    private String resizePlab;

    /**
     * Option to enable/disable adaptive TLAB sizing.
     * 
     * <pre>
     *-XX:+ResizeTLAB
     * </pre>
     */
    private String resizeTlab;

    /**
     * Option to disable JVM signal handling. For example:
     * 
     * <pre>
     * -Xrs
     * </pre>
     */
    private boolean rs = false;

    /**
     * JVM option to load the Java Debug Wire Protocol (JDWP) library. Equivalent to -agentlib:jdwp.
     * 
     * For example:
     * 
     * -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n
     */
    private ArrayList<String> runjdwp = new ArrayList<String>();

    /**
     * Option to enable the server JIT compiler, a separate Java binary, optimized for overall performance. The only JIT
     * compiler available on 64-bit. For example:
     * 
     * <pre>
     * -server
     * </pre>
     */
    private boolean server = false;

    /**
     * Option to define Shenandoah heuristics. Heuristics tell Shenandoah when to start the GC cycle and what regions to
     * use for evacuation. Some heuristics accept additional configuration options to tailor GC to specific use cases.
     * 
     * For example:
     * 
     * <p>
     * 1) adaptive (default)
     * </p>
     * 
     * <pre>
     * -XX:ShenandoahGCHeuristics=adaptive
     * 
     * -XX:ShenandoahInitFreeThreshold=#
     * -XX:ShenandoahMinFreeThreshold=# 
     * -XX:ShenandoahAllocSpikeFactor=# 
     * -XX:ShenandoahGarbageThreshold=#
     * </pre>
     * 
     * <p>
     * 2) static
     * </p>
     * 
     * <pre>
     * -XX:ShenandoahGCHeuristics=static
     * 
     * -XX:ShenandoahInitFreeThreshold=# 
     * -XX:ShenandoahGarbageThreshold=#
     * </pre>
     * 
     * <p>
     * 3) compact
     * </p>
     * 
     * <pre>
     * -XX:ShenandoahGCHeuristics=compact
     * 
     * -XX:ConcGCThreads=#
     * -XX:ShenandoahAllocationThreshold=#
     * </pre>
     * 
     * <p>
     * 4) aggressive
     * </p>
     * 
     * <pre>
     * -XX:ShenandoahGCHeuristics=aggressive
     * </pre>
     */
    private String shenandoahGcHeuristics;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) to specify the number of
     * milliseconds for a guaranteed GC cycle.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGuaranteedGCInterval=20000
     * </pre>
     */
    private String shenandoahGuaranteedGCInterval;

    /**
     * The minimum percentage of free space at which heuristics triggers the GC unconditionally. For example:
     * 
     * <pre>
     * -XX:ShenandoahMinFreeThreshold=10
     * </pre>
     */
    private String shenandoahMinFreeThreshold;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) to specify the number of
     * milliseconds before unused memory in the page cache is evicted (default 5 minutes). Setting below 1 second can
     * cause allocation stalls. For example:
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:ShenandoahUncommitDelay=5000
     * </pre>
     */
    private String shenandoahUncommitDelay;

    /**
     * Option to set the time in milliseconds (1000 default) per free megabyte in the heap that a softly reachable
     * object is kept active on the heap after the last time it is referenced. For example:
     * 
     * <pre>
     * -XX:SoftRefLRUPolicyMSPerMB=10
     * </pre>
     */
    private String softRefLRUPolicyMSPerMB;

    /**
     * Option to set the number of <code>String</code>s to pool in the String table to optimize memory.
     * 
     * The String.intern() method is used to intern a String object and store it inside the string pool for reuse. This
     * is done automatically at compile time.
     * 
     * The String table is in the permanent generation in JDK 6/7 and moved to the Java heap in JDK8.
     * 
     * The default size is 1009 (JDK6) and 60013 (JDK7+).
     * 
     * <pre>
     * -XX:StringTableSize=1000003
     * </pre>
     */
    private String stringTableSize;

    /**
     * The option for setting the size of the eden space compared to ONE survivor space. For example:
     * 
     * <pre>
     * -XX:SurvivorRatio=6
     * </pre>
     */
    private String survivorRatio;

    /**
     * JVM options used to define system properties.
     * 
     * For example:
     * 
     * -Dcatalina.base=/path/to/tomcat
     */
    private ArrayList<String> systemProperties = new ArrayList<String>();

    /**
     * The option for setting the percentage of the survivor space allowed to be occupied. For example:
     * 
     * <pre>
     * -XX:TargetSurvivorRatio=90
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String targetSurvivorRatio;

    /**
     * The option for setting the the Java Threads API policy to one of two values:
     * 
     * 0: Normal (default):
     * 
     * Linux: Thread priorities are ignored altogether.
     * 
     * Windows: Allowed to use higher native priorities, with the exception of THREAD_PRIORITY_TIME_CRITICAL.
     * 
     * Solaris: Priorities below NORM_PRIORITY are mapped to lower native priorities, higher than NORM_PRIORITY are
     * mapped to normal native priority.
     * 
     * 1: Aggressive:
     * 
     * Thread priorities map to the entire range of thread priorities. Should be used with care, as it can cause
     * performance degradation in the application and/or the entire system.
     * 
     * Requires root access on Linux. There was a bug in JDK8 where only -XX:ThreadPriorityPolicy=1 was checked for root
     * permissions, allowing non-root user to set the aggressive policy with a value other than 1 (e.g.
     * -XX:ThreadPriorityPolicy=42). This back door has been closed in JDK11, which only allows values of 0 or 1.
     * 
     * For example:
     * 
     * <pre>
     * -XX:ThreadPriorityPolicy=1
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String threadPriorityPolicy;

    /**
     * Thread stack size. Specified with either <code>-Xss</code> or <code>-ss</code> with optional units [kKmMgG] or
     * <code>-XX:ThreadStackSize</code> with optional units [kKmMgG] representing kilobytes. For example:
     * 
     * <pre>
     * -Xss524288 (524288 bytes)
     * </pre>
     * 
     * <pre>
     * -Xss256k (256 kilobytes)
     * </pre>
     * 
     * <pre>
     * -XX:ThreadStackSize=128 (128 _kilobytes_)
     * </pre>
     * 
     * <pre>
     * -XX:ThreadStackSize=128k (128 _megabytes_)
     * </pre>
     * 
     * The <code>-Xss</code> option does not work on Solaris, only <code>-XX:ThreadStackSize</code>.
     * 
     */
    private String threadStackSize;

    /**
     * The option for setting the number of method executions before a method is compiled with the C1 (client) compiler
     * with invocation and backedge counters.
     * 
     * For example:
     * 
     * <pre>
     * -XX:Tier2CompileThreshold=2000
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String tier2CompileThreshold;

    /**
     * The option for setting the number of method executions before a method is compiled with the C1 (client) compiler
     * with full profiling.
     * 
     * For example:
     * 
     * <pre>
     * -XX:Tier3CompileThreshold=2000
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String tier3CompileThreshold;

    /**
     * The option for setting the number of method executions before a method is compiled with the C2 (server) compiler.
     * 
     * For example:
     * 
     * <pre>
     * -XX:Tier4CompileThreshold=15000
     * </pre>
     * 
     * @return the option if it exists, null otherwise.
     */
    private String tier4CompileThreshold;

    /**
     * Option to enable/disable tiered compilation. Introduced in JDK7 and enabled by default in JDK8+
     * 
     * The JVM contains 2 Just in Time (JIT) compilers:
     * 
     * C1: Called the "client" compiler because it was originally designed with GUI applications in mind, where fast
     * startup is required.
     * 
     * C2: Called the "server" compiler because it was originally designed with long running server applications in
     * mind, aggressive optimization and performance is required.
     * 
     * The C1 compiler first compiles the code quickly to provide better startup performance. After the application is
     * warmed up, the C2 compiler compiles the code again with more aggressive optimizations for better performance.
     * 
     * It typically improves performance; however, there are the following use cases for disabling it:
     * 
     * <ul>
     * <li>Small memory footprint use cases (e.g. container).</li>
     * <li>It causes performance issues (e.g. <a href=
     * "https://bugzilla.redhat.com/show_bug.cgi?id=1420222">https://bugzilla.redhat.com/show_bug.cgi?id=1420222</a>)
     * </li>
     * </ul>
     * 
     * For example:
     * 
     * <pre>
     * -XX:+TieredCompilation
     * </pre>
     */
    private String tieredCompilation;

    /**
     * Option to enable/disable tracing class loading. Removed in JDK17. For example:
     * 
     * <pre>
     * -XX:+TraceClassLoading.
     * </pre>
     */
    private String traceClassLoading;

    /**
     * Option to enable/disable class loading/unloading information in gc log. Removed JDK17. For example:
     * 
     * <pre>
     * -XX:-TraceClassUnloading
     * </pre>
     */
    private String traceClassUnloading;

    /**
     * Undefined JVM options.
     */
    private ArrayList<String> undefined = new ArrayList<String>();

    /**
     * Option to enable/disable diagnostic options. For example:
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions
     * </pre>
     */
    private String unlockDiagnosticVmOptions;

    /**
     * Option to enable/disable experimental options. For example:
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions
     * </pre>
     */
    private String unlockExperimentalVmOptions;

    /**
     * Diagnostic option (-requires <code>XX:+UnlockDiagnosticVMOptions</code>) to enable/disable parallel class
     * loading. Disabled by default and deprecated in JDK11.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass
     * </pre>
     */
    private String unsyncloadClass;

    /**
     * Option to enable/disable ergonomic option that resizes generations to meet pause and throughput goals and
     * minimize footprint. For example:
     * 
     * <pre>
     * -XX:+UseAdaptiveSizePolicy
     * </pre>
     */
    private String useAdaptiveSizePolicy;

    /**
     * The compiler AVX instruction set level. For example:
     * 
     * <pre>
     * -XX:UseAVX=0 (disabled)
     * </pre>
     */
    private String useAvx;

    /**
     * Option to enable/disable biased locking. For example:
     * 
     * <pre>
     * -XX:-UseBiasedLocking
     * </pre>
     */
    private String useBiasedLocking;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) option to enable/disabled cgroup
     * memory limit for heap sizing.
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap
     * </pre>
     */
    private String useCGroupMemoryLimitForHeap;

    /**
     * Option to enable/disable full collections being handled by the concurrent CMS collector, a normal "background"
     * collection being run in the "foreground" within a safepoint. It is very slow and has no real use cases, so was
     * deprecated in JDK8 and removed in JDK9.
     * 
     * It is disabled by default (the SERIAL_OLD collector is used).
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UseCMSCompactAtFullCollection
     * </pre>
     */
    private String useCmsCompactAtFullCollection;

    /**
     * The option for disabling heuristics (calculating anticipated promotions) and use only the occupancy fraction to
     * determine when to trigger a CMS cycle. When an application has large variances in object allocation and young
     * generation promotion rates, the CMS collector is not able to accurately predict when to start the CMS cycle. For
     * example:
     * 
     * <pre>
     * -XX:+UseCMSInitiatingOccupancyOnly
     * </pre>
     */
    private String useCmsInitiatingOccupancyOnly;

    /**
     * Option to enable/disable code cache flushing. For example:
     * 
     * <pre>
     * -XX:+UseCodeCacheFlushing
     * </pre>
     */
    private String useCodeCacheFlushing;

    /**
     * Option to enable/disable compressed class pointers. For example:
     * 
     * <pre>
     * -XX:-UseCompressedClassPointers
     * </pre>
     */
    private String useCompressedClassPointers;

    /**
     * Option to enable/disable compressed object pointers. For example:
     * 
     * <pre>
     * -XX:-UseCompressedOops
     * </pre>
     */
    private String useCompressedOops;

    /**
     * Option to enable/disable the CMS old collector. Deprecated in JDK9 and removed in JDK14.
     * 
     * For example:
     * 
     * -XX:+UseConcMarkSweepGC
     */
    private String useConcMarkSweepGc;

    /**
     * Option to enable/disable conditional dirty card marking.
     * 
     * For example:
     * 
     * -XX:+UseCondCardMark
     */
    private String useCondCardMark;

    /**
     * Option to enable/disable the JVM setting default threading based on container, not host, information by reading
     * cgroups cpu and memory settings. Added in JDK8 u191 and enabled by default.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UseContainerSupport
     * </pre>
     */
    private String useContainerSupport;

    /**
     * Option to enable/disable keeping safepoints in counted loops. For example:
     * 
     * <pre>
     * -XX:+UseCountedLoopSafepoints
     * </pre>
     */
    private String useCountedLoopSafepoints;

    /**
     * Option to enable/disable ergonomic option to manage the number of compiler threads. For example:
     * 
     * <pre>
     * -XX:+UseDynamicNumberOfCompilerThreads
     * </pre>
     */
    private String useDynamicNumberOfCompilerThreads;

    /**
     * Option to enable/disable ergonomic option to manage the number of parallel garbage collector threads. For
     * example:
     * 
     * <pre>
     * -XX:+UseDynamicNumberOfGCThreads
     * </pre>
     */
    private String useDynamicNumberOfGcThreads;

    /**
     * Option to enable/disable using optimized versions of Get<Primitive>Field. Removed in JDK11.
     * 
     * For example:
     * 
     * <pre>
     * -XX:-UseFastAccessorMethods
     * </pre>
     */
    private String useFastAccessorMethods;

    /**
     * Experimental option (requires <code>-XX:+UnlockExperimentalVMOptions</code>) to enable/disable fast unordered
     * timestamps in gc logging.
     * 
     * <pre>
     * -XX:+UnlockExperimentalVMOptions -XX:+UseFastUnorderedTimeStamps
     * </pre>
     */
    private String useFastUnorderedTimeStamps;

    /**
     * The option to enable/disable the G1 collector. G1 was made the default collector in JDK9.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UseG1GC
     * </pre>
     */
    private String useG1Gc;

    /**
     * Option to enable/disable gc log file rotation. For example:
     * 
     * -XX:+UseGCLogFileRotation
     */
    private String useGcLogFileRotation;

    /**
     * Option to enable/disable "OutOfMemoryError: GC overhead limit exceeded" when 98% of the total time is spent in
     * garbage collection and less than 2% of the heap is recovered. This feature is a throttle to prevent applications
     * from running for an extended period of time while making little or no progress because the heap is too small.
     * Enabled by default. Applies only to the CMS and Parallel collectors.
     * 
     * For example:
     * 
     * -XX:-UseGCOverheadLimit
     */
    private String useGcOverheadLimit;

    /**
     * Linux option equivalent to -XX:+UseLargePages to enable/disable the preallocation of all large pages on JVM
     * startup, preventing the JVM from growing/shrinking large pages memory areas. For example:
     * 
     * <pre>
     * -XX:+UseHugeTLBFS
     * </pre>
     */
    private String useHugeTLBFS;

    /**
     * Option to enable/disable large pages. For example:
     * 
     * -XX:+UseLargePages
     */
    private String useLargePages;

    /**
     * Option to enable/disable a workaround for Windows 2003. Disabled by default, and the workaround is no longer
     * needed in later releases.
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UseLargePagesIndividualAllocation
     * </pre>
     */
    private String useLargePagesIndividualAllocation;

    /**
     * The option to enable/disable a strict memory barrier. For example:
     * 
     * <pre>
     * -XX:+UseMembar
     * </pre>
     */
    private String useMembar;

    /**
     * Option to enable/disable dedicated memory space per processor. For example:
     * 
     * <pre>
     *-XX:+UseNUMA
     * </pre>
     */
    private String useNUMA;

    /**
     * Option to enable/disable the parallel scavenge young garbage collector. The parallel collector was made the
     * default collector in JDK7u4.
     * 
     * For example:
     * 
     * -XX:+UseParallelGC
     */
    private String useParallelGc;

    /**
     * Option to enable/disable the parallel multi-threaded old garbage collector. Redundant in JDK7/8/11, deprecated in
     * JDK15, and removed in JDK16.
     * 
     * Disabling it in combination with specifying the CMS young collector (e.g. -XX:+ParNew -XX:-UseParallelOldGC) has
     * the affect of using the CMS young collector in combination with the serial old collector.
     * 
     * For example:
     * 
     * -XX:+UseParallelOldGC
     */
    private String useParallelOldGc;

    /**
     * The option to enable/disable the CMS young collector. The use case for this option is to -disable the CMS young
     * (parallel) collector with -XX:-UseParNewGC to force using the serial new collector.
     * 
     * Deprecated in JDK8 and removed in JDK9 (i.e. you can only use the CMS young collector in combination with the CMS
     * old collector in JDK9+).
     * 
     * For example:
     * 
     * <pre>
     * -XX:+UseParNewGC
     * </pre>
     */
    private String useParNewGc;

    /**
     * The option to enable/disable outputting performance data to disk (/tmp/hsperfdata*) and via JMX. For example:
     * 
     * <pre>
     * -XX:-UsePerfData
     * </pre>
     */
    private String usePerfData;

    /**
     * Option to enable/disable the Serial garbage collector. For example:
     * 
     * -XX:+UseSerialGC
     */
    private String useSerialGc;

    /**
     * Option to enable/disable the Shenandoah garbage collector. For example:
     * 
     * -XX:+UseShenandoahGC
     */
    private String useShenandoahGc;

    /**
     * The option to enable/disable using a new type checker introduced in JDK5 with StackMapTable attributes. Mandatory
     * in JDK8+. Ignored in JDK8 and unrecognized in JDK11+.
     * 
     * <pre>
     * -XX:-UseSplitVerifier
     * </pre>
     */
    private String useSplitVerifier;

    /**
     * The option to enable/disable string deduplication to minimize string footprint. The performance impact is minimal
     * (some cpu cycles to run the concurrent deduplication process).
     * 
     * <pre>
     * -XX:+UseStringDeduplication
     * </pre>
     */
    private String useStringDeduplication;

    /**
     * Option to enable(default)/disable the use of the Java Threads API (e.g. java.lang.Thread.setPriority() can be
     * used to manually set thread priority to override the JVM thread scheduler default priority).
     * 
     * When disabled, any Java Threads API calls have no affect.
     * 
     * For example:
     * 
     * <pre>
     * -XX:-UseThreadPriorities
     * </pre>
     */
    private String useThreadPriorities;

    /**
     * Option to enable/disable a thread-local allocation buffer. Using thread-local object allocation blocks improve
     * concurrency by reducing contention on the shared heap lock, allowing for more scalable allocation for heavily
     * threaded applications, increasing allocation performance. Enabled by default on multiprocessor systems.
     * 
     * <pre>
     * -XX:+UseTLAB
     * </pre>
     */
    private String useTlab;

    /**
     * Option to enable/disable making Solaris interruptible blocking I/O noninterruptible as they are on Linux and
     * Windows. Deprecated in JDK8 and removed in JDK11.
     */
    private String useVmInterruptibleIo;

    /**
     * Option to enable/disable the Z garbage collector (ZGC). For example:
     * 
     * -XX:+UseZGC
     */
    private String useZGc;

    /**
     * Option to enable logging (to standard out) class loading information.
     * 
     * -verbose:class
     */
    private boolean verboseClass = false;

    /**
     * Option to enable displaying detailed information about each gc event. Equivalent to <code>-XX:+PrintGC</code>.
     * 
     * -verbose:gc
     */
    private boolean verboseGc = false;

    /**
     * Option to specify class verification during class loading.
     * 
     * For example:
     * 
     * <pre>
     * -Xverify
     * -Xverify:all
     * -Xverify:none
     * -Xverify:remote
     * </pre>
     */
    private String verify;

    /**
     * Option to disable just in time (JIT) compilation. For example:
     * 
     * <pre>
     * -Xint
     * </pre>
     */
    private boolean xInt = false;

    /**
     * Parse JVM arguments and do analysis.
     * 
     * @param jvmContext
     *            The JVM context.
     */
    public JvmOptions(JvmContext jvmContext) {
        this.jvmContext = jvmContext;
        if (jvmContext.getOptions() != null) {
            // (?<!^) match whatever follows, but not the start of the string
            // (?= -) match "space dash" followed by jvm option starting patterns, but don't
            // include the empty leading
            // substring in the result
            String[] options = jvmContext.getOptions().split(
                    "(?<!^)(?= -(-add|agentlib|agentpath|classpath|client|d(32|64)|javaagent|noverify|server|verbose|D|"
                            + "X))");
            String key = null;
            for (int i = 0; i < options.length; i++) {
                String option = options[i].trim();
                if (option.matches("^--add-exports=.+$")) {
                    addExports.add(option);
                    key = option;
                } else if (option.matches("^--add-modules=.+$")) {
                    addModules.add(option);
                    key = option;
                } else if (option.matches("^--add-opens=.+$")) {
                    addOpens.add(option);
                    key = option;
                } else if (option.matches("^-agentlib:.+$")) {
                    agentlib.add(option);
                    key = "agentlib";
                } else if (option.matches("^-agentpath:.+$")) {
                    agentpath.add(option);
                    key = "agentpath";
                } else if (option.matches("^-classpath.+$")) {
                    classpath = option;
                    key = "classpath";
                } else if (option.matches("^-client$")) {
                    client = true;
                    key = "client";
                } else if (option.matches("^-d64$")) {
                    d64 = true;
                    key = "d64";
                } else if (option.matches("^-javaagent:.+$")) {
                    javaagent.add(option);
                    key = option;
                } else if (option.matches("^-noverify$")) {
                    noverify = true;
                    key = "noverify";
                } else if (option.matches("^-server$")) {
                    server = true;
                    key = "server";
                } else if (option.matches("^-verbose:class$")) {
                    verboseClass = true;
                    key = "class";
                } else if (option.matches("^-verbose:gc$")) {
                    verboseGc = true;
                    key = "verbose";
                } else if (option.matches("^-D.+$")) {
                    systemProperties.add(option);
                    key = "D";
                } else if (option.matches("^-Xbatch$")) {
                    batch = true;
                    key = "batch";
                } else if (option.matches("^-Xbootclasspath.+$")) {
                    bootclasspath.add(option);
                    key = "Xbootclasspath";
                } else if (option.matches("^-Xcheck:jni$")) {
                    checkJni = true;
                    key = "check:jni";
                } else if (option.matches("^-Xcomp$")) {
                    comp = true;
                    key = "comp";
                } else if (option.matches("^-Xconcurrentio$")) {
                    concurrentio = true;
                    key = "concurrentio";
                } else if (option.matches("^-Xdebug$")) {
                    debug = true;
                    key = "debug";
                } else if (option.matches("^-Xint$")) {
                    xInt = true;
                    key = "int";
                } else if (option.matches("^-Xlog:.+$")) {
                    log.add(option);
                    key = "log";
                } else if (option.matches("^-Xloggc:.+$")) {
                    loggc = option;
                    key = "loggc";
                } else if (option.matches("^-Xmaxjitcodesize\\d{1,}[kKmMgG]{0,1}$")) {
                    maxjitcodesize = option;
                    key = "maxjitcodesize";
                } else if (option.matches("^-X(mn|X:NewSize=)" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    newSize = option;
                    key = "NewSize";
                } else if (option.matches("^-X(ms|X:InitialHeapSize=)" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    initialHeapSize = option;
                    key = "InitialHeapSize";
                } else if (option.matches("^-X(mx|X:MaxHeapSize=)" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    maxHeapSize = option;
                    key = "MaxHeapSize";
                } else if (option.matches("^-Xnoclassgc$")) {
                    noclassgc = true;
                    key = "noclassgc";
                } else if (option.matches("^-Xrs$")) {
                    rs = true;
                    key = "rs";
                } else if (option.matches("^-Xrunjdwp:.+$")) {
                    runjdwp.add(option);
                    key = "runjdwp";
                } else if (option.matches("^-Xverify(:(all|none|remote))?$")) {
                    verify = option;
                    key = "verify";
                } else if (option.matches("^-XX:ActiveProcessorCount=\\d{1,}$")) {
                    activeProcessorCount = option;
                    key = "ActiveProcessorCount";
                } else if (option.matches("^-XX:AdaptiveSizePolicyWeight=\\d{1,3}$")) {
                    adaptiveSizePolicyWeight = option;
                    key = "AdaptiveSizePolicyWeight";
                } else if (option.matches("^-XX:[\\-+]AggressiveHeap$")) {
                    aggressiveHeap = option;
                    key = "AggressiveHeap";
                } else if (option.matches("^-XX:[\\-+]AggressiveOpts$")) {
                    aggressiveOpts = option;
                    key = "AggressiveOpts";
                } else if (option.matches("^-XX:[\\-+]AlwaysPreTouch$")) {
                    alwaysPreTouch = option;
                    key = "alwaysPreTouch";
                } else if (option.matches("^-XX:AutoBoxCacheMax=\\d{1,10}$")) {
                    autoBoxCacheMax = option;
                    key = "autoBoxCacheMax";
                } else if (option.matches("^-XX:[\\-+]BackgroundCompilation$")) {
                    backgroundCompilation = option;
                    key = "BackgroundCompilation";
                } else if (option.matches("^-XX:CICompilerCount=\\d{1,3}$")) {
                    ciCompilerCount = option;
                    key = "CICompilerCount";
                } else if (option.matches("^-XX:[\\-+]ClassUnloading$")) {
                    classUnloading = option;
                    key = "ClassUnloading";
                } else if (option.matches("^-XX:[\\-+]CMSEdenChunksRecordAlways$")) {
                    cmsEdenChunksRecordAlways = option;
                    key = "CMSEdenChunksRecordAlways";
                } else if (option.matches("^-XX:[\\-+]CMSClassUnloadingEnabled$")) {
                    cmsClassUnloadingEnabled = option;
                    key = "CMSClassUnloadingEnabled";
                } else if (option.matches("^-XX:[\\-+]CMSIncrementalMode$")) {
                    cmsIncrementalMode = option;
                    key = "CMSIncrementalMode";
                } else if (option.matches("^-XX:CMSIncrementalSafetyFactor=\\d{1,3}$")) {
                    cmsIncrementalSafetyFactor = option;
                    key = "CMSIncrementalSafetyFactor";
                } else if (option.matches("^-XX:CMSInitiatingOccupancyFraction=\\d{1,3}$")) {
                    cmsInitiatingOccupancyFraction = option;
                    key = "CMSInitiatingOccupancyFraction";
                } else if (option.matches("^-XX:[\\-+]CMSParallelInitialMarkEnabled$")) {
                    cmsParallelInitialMarkEnabled = option;
                    key = "CMSParallelInitialMarkEnabled";
                } else if (option.matches("^-XX:[\\-+]CMSParallelRemarkEnabled$")) {
                    cmsParallelRemarkEnabled = option;
                    key = "CMSParallelRemarkEnabled";
                } else if (option.matches("^-XX:[\\-+]CMSScavengeBeforeRemark$")) {
                    cmsScavengeBeforeRemark = option;
                    key = "CMSScavengeBeforeRemark";
                } else if (option.matches("^-XX:CMSWaitDuration=\\d{1,}$")) {
                    cmsWaitDuration = option;
                    key = "CMSWaitDuration";
                } else if (option.matches("^-XX:CompileCommand=.+$")) {
                    compileCommand = option;
                    key = "CompileCommand";
                } else if (option.matches("^-XX:CompileCommandFile=.+$")) {
                    compileCommandFile = option;
                    key = "CompileCommandFile";
                } else if (option.matches("^-XX:CompileThreshold=\\d{1,}$")) {
                    compileThreshold = option;
                    key = "CompileThreshold";
                } else if (option.matches("^-XX:CompressedClassSpaceSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    compressedClassSpaceSize = option;
                    key = "CompressedClassSpaceSize";
                } else if (option.matches("^-XX:ConcGCThreads=\\d{1,3}$")) {
                    concGcThreads = option;
                    key = "ConcGCThreads";
                } else if (option.matches("^-XX:[\\-+]CrashOnOutOfMemoryError$")) {
                    crashOnOutOfMemoryError = option;
                    key = "CrashOnOutOfMemoryError";
                } else if (option.matches("^-XX:[\\-+]DebugNonSafepoints$")) {
                    debugNonSafepoints = option;
                    key = "DebugNonSafepoints";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:[\\-+]DisableAttachMechanism$")) {
                    disableAttachMechanism = option;
                    key = "DisableAttachMechanism";
                } else if (option.matches("^-XX:[\\-+]DisableExplicitGC$")) {
                    disableExplicitGc = option;
                    key = "DisableExplicitGC";
                } else if (option.matches("^-XX:[\\-+]DoEscapeAnalysis$")) {
                    doEscapeAnalysis = option;
                    key = "DoEscapeAnalysis";
                } else if (option.matches("^-XX:[\\-+]EliminateLocks$")) {
                    eliminateLocks = option;
                    key = "EliminateLocks";
                } else if (option.matches("^-XX:ErrorFile=\\S+$")) {
                    errorFile = option;
                    key = "ErrorFile";
                } else if (option.matches("^-XX:[\\-+]ExitOnOutOfMemoryError$")) {
                    exitOnOutOfMemoryError = option;
                    key = "ExitOnOutOfMemoryError";
                } else if (option.matches("^-XX:[\\-+]ExplicitGCInvokesConcurrent$")) {
                    explicitGCInvokesConcurrent = option;
                    key = "ExplicitGCInvokesConcurrent";
                } else if (option.matches("^-XX:[\\-+]ExplicitGCInvokesConcurrentAndUnloadsClasses$")) {
                    explicitGCInvokesConcurrentAndUnloadsClasses = option;
                    key = "ExplicitGCInvokesConcurrentAndUnloadsClasses";
                } else if (option.matches("^-XX:[\\-+]ExtensiveErrorReports$")) {
                    extensiveErrorReports = option;
                    key = "ExtensiveErrorReports";
                } else if (option.matches("^-XX:[\\-+]MaxFDLimit$")) {
                    maxFdLimit = option;
                    key = "MaxFDLimit ";
                } else if (option.matches("^-XX:FlightRecorderOptions=.+$")) {
                    flightRecorderOptions = option;
                    key = "FlightRecorderOptions";
                } else if (option.matches("^-XX:G1ConcRefinementThreads=\\d{1,}$")) {
                    g1ConcRefinementThreads = option;
                    key = "G1ConcRefinementThreads";
                } else if (option.matches("^-XX:G1HeapRegionSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    g1HeapRegionSize = option;
                    key = "G1HeapRegionSize";
                } else if (option.matches("^-XX:G1HeapWastePercent=\\d{1,3}$")) {
                    g1HeapWastePercent = option;
                    key = "G1HeapWastePercent";
                } else if (option.matches("^-XX:G1MaxNewSizePercent=\\d{1,3}$")) {
                    g1MaxNewSizePercent = option;
                    key = "G1MaxNewSizePercent";
                    experimental.add(option);
                } else if (option.matches("^-XX:G1MixedGCCountTarget=\\d{1,}$")) {
                    g1MixedGCCountTarget = option;
                    key = "G1MixedGCCountTarget";
                } else if (option.matches("^-XX:G1MixedGCLiveThresholdPercent=\\d{1,3}$")) {
                    g1MixedGCLiveThresholdPercent = option;
                    key = "G1MixedGCLiveThresholdPercent";
                    experimental.add(option);
                } else if (option.matches("^-XX:G1NewSizePercent=\\d{1,3}$")) {
                    g1NewSizePercent = option;
                    key = "G1NewSizePercent";
                    experimental.add(option);
                } else if (option.matches("^-XX:G1ReservePercent=\\d{1,3}$")) {
                    g1ReservePercent = option;
                    key = "G1ReservePercent";
                } else if (option.matches("^-XX:[\\-+]G1SummarizeRSetStats$")) {
                    g1SummarizeRSetStats = option;
                    key = "G1SummarizeRSetStats";
                } else if (option.matches("^-XX:G1SummarizeRSetStatsPeriod=\\d$")) {
                    g1SummarizeRSetStatsPeriod = option;
                    key = "G1SummarizeRSetStatsPeriod";
                } else if (option.matches("^-XX:GCLockerRetryAllocationCount=\\d{1,}$")) {
                    gcLockerRetryAllocationCount = option;
                    key = "GCLockerRetryAllocationCount";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:GCLogFileSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    gcLogFileSize = option;
                    key = "GCLogFileSize";
                } else if (option.matches("^-XX:GCTimeRatio=\\d{1,3}$")) {
                    gcTimeRatio = option;
                    key = "GCTimeRatio";
                } else if (option.matches("^-XX:GuaranteedSafepointInterval=\\d{1,10}$")) {
                    guaranteedSafepointInterval = option;
                    key = "GuaranteedSafepointInterval";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:HeapBaseMinAddress=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    heapBaseMinAddress = option;
                    key = "HeapBaseMinAddress";
                } else if (option.matches("^-XX:[\\-+]HeapDumpOnOutOfMemoryError$")) {
                    heapDumpOnOutOfMemoryError = option;
                    key = "HeapDumpOnOutOfMemoryError";
                } else if (option.matches("^-XX:HeapDumpPath=\\S+$")) {
                    heapDumpPath = option;
                    key = "HeapDumpPath";
                } else if (option.matches("^-XX:[\\-+]IgnoreUnrecognizedVMOptions$")) {
                    ignoreUnrecognizedVmOptions = option;
                    key = "IgnoreUnrecognizedVMOptions";
                } else if (option
                        .matches("^-XX:InitialBootClassLoaderMetaspaceSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    initialBootClassLoaderMetaspaceSize = option;
                    key = "InitialBootClassLoaderMetaspaceSize";
                } else if (option.matches("^-XX:InitiatingHeapOccupancyPercent=\\d{1,3}$")) {
                    initiatingHeapOccupancyPercent = option;
                    key = "InitiatingHeapOccupancyPercent";
                } else if (option.matches("^-XX:LargePageSizeInBytes=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    largePageSizeInBytes = option;
                    key = "LargePageSizeInBytes";
                } else if (option.matches("^-XX:LogFile=\\S+$")) {
                    logFile = option;
                    key = "LogFile";
                } else if (option.matches("^-XX:[\\-+]LogVMOutput$")) {
                    logVmOutput = option;
                    key = "LogVMOutput";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:LoopStripMiningIter=\\d{1,}$")) {
                    loopStripMiningIter = option;
                    key = "LoopStripMiningIter";
                } else if (option.matches("^-XX:MaxGCPauseMillis=\\d{1,}$")) {
                    maxGcPauseMillis = option;
                    key = "MaxGCPauseMillis";
                } else if (option.matches("^-XX:MaxJavaStackTraceDepth=\\d{1,}$")) {
                    maxJavaStackTraceDepth = option;
                    key = "MaxJavaStackTraceDepth";
                } else if (option.matches("^-XX:MaxNewSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    maxNewSize = option;
                    key = "MaxNewSize";
                } else if (option.matches("^-XX:MetaspaceSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    metaspaceSize = option;
                    key = "MetaspaceSize";
                } else if (option.matches("^-XX:[\\-+]ManagementServer$")) {
                    managementServer = option;
                    key = "ManagementServer";
                } else if (option.matches("^-XX:MarkStackSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    markStackSize = option;
                    key = "MarkStackSize";
                } else if (option.matches("^-XX:MarkStackSizeMax=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    markStackSizeMax = option;
                    key = "MarkStackSizeMax";
                } else if (option.matches("^-XX:MaxDirectMemorySize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    maxDirectMemorySize = option;
                    key = "MaxDirectMemorySize";
                } else if (option.matches("^-XX:MaxHeapFreeRatio=\\d{1,3}$")) {
                    maxHeapFreeRatio = option;
                    key = "MaxHeapFreeRatio";
                } else if (option.matches("^-XX:MaxInlineLevel=\\d{1,}$")) {
                    maxInlineLevel = option;
                    key = "MaxInlineLevel";
                } else if (option.matches("^-XX:MaxMetaspaceSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    maxMetaspaceSize = option;
                    key = "MaxMetaspaceSize";
                } else if (option.matches("^-XX:MaxPermSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    maxPermSize = option;
                    key = "MaxPermSize";
                } else if (option.matches("^-XX:MaxTenuringThreshold=\\d{1,}$")) {
                    maxTenuringThreshold = option;
                    key = "MaxTenuringThreshold";
                } else if (option.matches("^-XX:MinHeapDeltaBytes=\\d{1,}$")) {
                    minHeapDeltaBytes = option;
                    key = "MinHeapDeltaBytes";
                } else if (option.matches("^-XX:MinHeapFreeRatio=\\d{1,3}$")) {
                    minHeapFreeRatio = option;
                    key = "MinHeapFreeRatio";
                } else if (option.matches("^-XX:NativeMemoryTracking=.+$")) {
                    nativeMemoryTracking = option;
                    key = "NativeMemoryTracking";
                } else if (option.matches("^-XX:NewRatio=.+$")) {
                    newRatio = option;
                    key = "NewRatio";
                } else if (option.matches("^-XX:NumberOfGCLogFiles=\\d{1,}$")) {
                    numberOfGcLogFiles = option;
                    key = "NumberOfGCLogFiles";
                } else if (option.matches("^-XX:OldPLABSize=\\d{1,}$")) {
                    oldPlabSize = option;
                    key = "OldPLABSize";
                } else if (option.matches("^-XX:OldSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    oldSize = option;
                    key = "OldSize";
                } else if (option.matches("^-XX:[\\-+]OmitStackTraceInFastThrow$")) {
                    omitStackTraceInFastThrow = option;
                    key = "OmitStackTraceInFastThrow";
                } else if (option.matches("^-XX:OnError=.+$")) {
                    onError = option;
                    key = "OnError";
                } else if (option.matches("^-XX:OnOutOfMemoryError=.+$")) {
                    onOutOfMemoryError = option;
                    key = "OnOutOfMemoryError";
                } else if (option.matches("^-XX:[\\-+]OptimizeStringConcat$")) {
                    optimizeStringConcat = option;
                    key = "OptimizeStringConcat";
                } else if (option.matches("^-XX:ParallelGCThreads=\\d{1,3}$")) {
                    parallelGcThreads = option;
                    key = "ParallelGCThreads";
                } else if (option.matches("^-XX:[\\-+]ParallelRefProcEnabled$")) {
                    parallelRefProcEnabled = option;
                    key = "ParallelRefProcEnabled";
                } else if (option.matches("^-XX:[\\-+]PerfDisableSharedMem$")) {
                    perfDisableSharedMem = option;
                    key = "PerfDisableSharedMem";
                } else if (option.matches("^-XX:PerMethodRecompilationCutoff=\\d{1,}$")) {
                    perMethodRecompilationCutoff = option;
                    key = "PerMethodRecompilationCutoff";
                } else if (option.matches("^-XX:PermSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    permSize = option;
                    key = "PermSize";
                } else if (option.matches("^-XX:[\\-+]PrintAdaptiveSizePolicy$")) {
                    printAdaptiveSizePolicy = option;
                    key = "PrintAdaptiveSizePolicy";
                } else if (option.matches("^-XX:[\\-+]PrintClassHistogram$")) {
                    printClassHistogram = option;
                    key = "PrintClassHistogram";
                } else if (option.matches("^-XX:[\\-+]PrintClassHistogramAfterFullGC$")) {
                    printClassHistogramAfterFullGc = option;
                    key = "PrintClassHistogramAfterFullGC";
                } else if (option.matches("^-XX:[\\-+]PrintClassHistogramBeforeFullGC$")) {
                    printClassHistogramBeforeFullGc = option;
                    key = "PrintClassHistogramBeforeFullGC";
                } else if (option.matches("^-XX:[\\-+]PrintCodeCache$")) {
                    printCodeCache = option;
                    key = "PrintCodeCache";
                } else if (option.matches("^-XX:[\\-+]PrintCommandLineFlags$")) {
                    printCommandLineFlags = option;
                    key = "PrintCommandLineFlags";
                } else if (option.matches("^-XX:[\\-+]PrintFlagsFinal$")) {
                    printFlagsFinal = option;
                    key = "PrintFlagsFinal";
                } else if (option.matches("^-XX:PrintFLSStatistics=\\d$")) {
                    printFLSStatistics = option;
                    key = "PrintFLSStatistics";
                } else if (option.matches("^-XX:[\\-+]PrintGC$")) {
                    printGc = option;
                    key = "PrintGC";
                } else if (option.matches("^-XX:[\\-+]PrintGCApplicationConcurrentTime$")) {
                    printGcApplicationConcurrentTime = option;
                    key = "PrintGCApplicationConcurrentTime";
                } else if (option.matches("^-XX:[\\-+]PrintGCApplicationStoppedTime$")) {
                    printGcApplicationStoppedTime = option;
                    key = "PrintGCApplicationStoppedTime";
                } else if (option.matches("^-XX:[\\-+]PrintGCCause$")) {
                    printGcCause = option;
                    key = "PrintGCCause";
                } else if (option.matches("^-XX:[\\-+]PrintGCDateStamps$")) {
                    printGcDateStamps = option;
                    key = "PrintGCDateStamps";
                } else if (option.matches("^-XX:[\\-+]PrintGCDetails$")) {
                    printGcDetails = option;
                    key = "PrintGCDetails";
                } else if (option.matches("^-XX:[\\-+]PrintGCTaskTimeStamps$")) {
                    printGcTaskTimeStamps = option;
                    key = "PrintGCTaskTimeStamps";
                } else if (option.matches("^-XX:[\\-+]PrintGCTimeStamps$")) {
                    printGcTimeStamps = option;
                    key = "PrintGCTimeStamps";
                } else if (option.matches("^-XX:[\\-+]PrintHeapAtGC$")) {
                    printHeapAtGc = option;
                    key = "PrintHeapAtGC";
                } else if (option.matches("^-XX:[\\-+]PrintPromotionFailure$")) {
                    printPromotionFailure = option;
                    key = "PrintPromotionFailure";
                } else if (option.matches("^-XX:[\\-+]PrintReferenceGC$")) {
                    printReferenceGc = option;
                    key = "PrintReferenceGC";
                } else if (option.matches("^-XX:[\\-+]PrintSafepointStatistics$")) {
                    printSafepointStatistics = option;
                    key = "PrintSafepointStatistics";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:[\\-+]PrintStringDeduplicationStatistics$")) {
                    printStringDeduplicationStatistics = option;
                    key = "PrintStringDeduplicationStatistics";
                } else if (option.matches("^-XX:[\\-+]PrintStringTableStatistics$")) {
                    printStringTableStatistics = option;
                    key = "PrintStringTableStatistics";
                } else if (option.matches("^-XX:[\\-+]PrintTenuringDistribution$")) {
                    printTenuringDistribution = option;
                    key = "PrintTenuringDistribution";
                } else if (option.matches("^-XX:ReservedCodeCacheSize=" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    reservedCodeCacheSize = option;
                    key = "ReservedCodeCacheSize";
                } else if (option.matches("^-XX:[\\-+]ResizePLAB$")) {
                    resizePlab = option;
                    key = "ResizePLAB";
                } else if (option.matches("^-XX:[\\-+]ResizeTLAB$")) {
                    resizeTlab = option;
                    key = "ResizeTLAB";
                } else if (option.matches("^-XX:ShenandoahGCHeuristics=(adaptive|aggressive|compact|static)$")) {
                    shenandoahGcHeuristics = option;
                    key = "ShenandoahGCHeuristics";
                } else if (option.matches("^-XX:ShenandoahGuaranteedGCInterval=\\d{1,}$")) {
                    shenandoahGuaranteedGCInterval = option;
                    key = "ShenandoahGuaranteedGCInterval";
                    experimental.add(option);
                } else if (option.matches("^-XX:ShenandoahMinFreeThreshold=\\d{1,3}$")) {
                    shenandoahMinFreeThreshold = option;
                    key = "ShenandoahMinFreeThreshold";
                } else if (option.matches("^-XX:ShenandoahUncommitDelay=\\d{1,}$")) {
                    shenandoahUncommitDelay = option;
                    key = "ShenandoahUncommitDelay";
                    experimental.add(option);
                } else if (option.matches("^-XX:SoftRefLRUPolicyMSPerMB=\\d{1,}$")) {
                    softRefLRUPolicyMSPerMB = option;
                    key = "SoftRefLRUPolicyMSPerMB";
                } else if (option.matches("^-XX:StringTableSize=\\d{1,}$")) {
                    stringTableSize = option;
                    key = "StringTableSize";
                } else if (option.matches("^-XX:SurvivorRatio=\\d{1,}$")) {
                    survivorRatio = option;
                    key = "SurvivorRatio";
                } else if (option.matches("^-(X)?(ss|X:ThreadStackSize=)" + JdkRegEx.OPTION_SIZE_BYTES + "$")) {
                    threadStackSize = option;
                    key = "ThreadStackSize";
                } else if (option.matches("^-XX:TargetSurvivorRatio=\\d{1,3}$")) {
                    targetSurvivorRatio = option;
                    key = "TargetSurvivorRatio";
                } else if (option.matches("^-XX:ThreadPriorityPolicy=[-]{0,1}\\d{1,}$")) {
                    threadPriorityPolicy = option;
                    key = "ThreadPriorityPolicy";
                } else if (option.matches("^-XX:Tier2CompileThreshold=\\d{1,}$")) {
                    tier2CompileThreshold = option;
                    key = "Tier2CompileThreshold";
                } else if (option.matches("^-XX:Tier3CompileThreshold=\\d{1,}$")) {
                    tier3CompileThreshold = option;
                    key = "Tier3CompileThreshold";
                } else if (option.matches("^-XX:Tier4CompileThreshold=\\d{1,}$")) {
                    tier4CompileThreshold = option;
                    key = "Tier4CompileThreshold";
                } else if (option.matches("^-XX:[\\-+]TieredCompilation$")) {
                    tieredCompilation = option;
                    key = "TieredCompilation";
                } else if (option.matches("^-XX:[\\-+]TraceClassLoading$")) {
                    traceClassLoading = option;
                    key = "TraceClassLoading";
                } else if (option.matches("^-XX:[\\-+]TraceClassUnloading$")) {
                    traceClassUnloading = option;
                    key = "TraceClassUnloading";
                } else if (option.matches("^-XX:[\\-+]UnlockDiagnosticVMOptions$")) {
                    unlockDiagnosticVmOptions = option;
                    key = "UnlockDiagnosticVMOptions";
                } else if (option.matches("^-XX:[\\-+]UnlockExperimentalVMOptions$")) {
                    unlockExperimentalVmOptions = option;
                    key = "UnlockExperimentalVMOptions";
                } else if (option.matches("^-XX:[\\-+]UnsyncloadClass$")) {
                    unsyncloadClass = option;
                    key = "UnsyncloadClass";
                    diagnostic.add(option);
                } else if (option.matches("^-XX:UseAVX=\\d{1,}$")) {
                    useAvx = option;
                    key = "UseAVX";
                } else if (option.matches("^-XX:[\\-+]UseAdaptiveSizePolicy$")) {
                    useAdaptiveSizePolicy = option;
                    key = "UseAdaptiveSizePolicy";
                } else if (option.matches("^-XX:[\\-+]UseBiasedLocking$")) {
                    useBiasedLocking = option;
                    key = "UseBiasedLocking";
                } else if (option.matches("^-XX:[\\-+]UseCGroupMemoryLimitForHeap$")) {
                    useCGroupMemoryLimitForHeap = option;
                    key = "UseCGroupMemoryLimitForHeap";
                    experimental.add(option);
                } else if (option.matches("^-XX:[\\-+]UseCMSCompactAtFullCollection$")) {
                    useCmsCompactAtFullCollection = option;
                    key = "UseCMSCompactAtFullCollection";
                } else if (option.matches("^-XX:[\\-+]UseCMSInitiatingOccupancyOnly$")) {
                    useCmsInitiatingOccupancyOnly = option;
                    key = "UseCMSInitiatingOccupancyOnly";
                } else if (option.matches("^-XX:[\\-+]UseCodeCacheFlushing$")) {
                    useCodeCacheFlushing = option;
                    key = "UseCodeCacheFlushing";
                } else if (option.matches("^-XX:[\\-+]UseCompressedClassPointers$")) {
                    useCompressedClassPointers = option;
                    key = "UseCompressedClassPointers";
                } else if (option.matches("^-XX:[\\-+]UseCompressedOops$")) {
                    useCompressedOops = option;
                    key = "UseCompressedOops";
                } else if (option.matches("^-XX:[\\-+]UseConcMarkSweepGC$")) {
                    useConcMarkSweepGc = option;
                    key = "UseConcMarkSweepGC";
                    // Overrides any previous serial old setting
                    if (JdkUtil.isOptionEnabled(useConcMarkSweepGc) && useParallelOldGc != null) {
                        useParallelOldGc = null;
                    }
                } else if (option.matches("^-XX:[\\-+]UseCondCardMark$")) {
                    useCondCardMark = option;
                    key = "UseCondCardMark";
                } else if (option.matches("^-XX:[\\-+]UseContainerSupport$")) {
                    useContainerSupport = option;
                    key = "UseContainerSupport";
                } else if (option.matches("^-XX:[\\-+]UseCountedLoopSafepoints$")) {
                    useCountedLoopSafepoints = option;
                    key = "UseCountedLoopSafepoints";
                } else if (option.matches("^-XX:[\\-+]UseDynamicNumberOfCompilerThreads$")) {
                    useDynamicNumberOfCompilerThreads = option;
                    key = "UseDynamicNumberOfCompilerThreads";
                } else if (option.matches("^-XX:[\\-+]UseDynamicNumberOfGCThreads$")) {
                    useDynamicNumberOfGcThreads = option;
                    key = "useDynamicNumberOfGcThreads";
                } else if (option.matches("^-XX:[\\-+]UseFastAccessorMethods$")) {
                    useFastAccessorMethods = option;
                    key = "UseFastAccessorMethods";
                } else if (option.matches("^-XX:[\\-+]UseFastUnorderedTimeStamps$")) {
                    useFastUnorderedTimeStamps = option;
                    key = "UseFastUnorderedTimeStamps";
                    experimental.add(option);
                } else if (option.matches("^-XX:[\\-+]UseG1GC$")) {
                    useG1Gc = option;
                    key = "UseG1GC";
                } else if (option.matches("^-XX:[\\-+]UseGCLogFileRotation$")) {
                    useGcLogFileRotation = option;
                    key = "UseGCLogFileRotation";
                } else if (option.matches("^-XX:[\\-+]UseGCOverheadLimit$")) {
                    useGcOverheadLimit = option;
                    key = "UseGCOverheadLimit";
                } else if (option.matches("^-XX:[\\-+]UseHugeTLBFS$")) {
                    useHugeTLBFS = option;
                    key = "UseHugeTLBFS";
                } else if (option.matches("^-XX:[\\-+]UseMembar$")) {
                    useMembar = option;
                    key = "UseMembar";
                } else if (option.matches("^-XX:[\\-+]UseLargePages$")) {
                    useLargePages = option;
                    key = "UseLargePages";
                } else if (option.matches("^-XX:[\\-+]UseLargePagesIndividualAllocation$")) {
                    useLargePagesIndividualAllocation = option;
                    key = "UseLargePagesIndividualAllocation";
                } else if (option.matches("^-XX:[\\-+]UseNUMA$")) {
                    useNUMA = option;
                    key = "UseNUMA";
                } else if (option.matches("^-XX:[\\-+]UseParallelGC$")) {
                    useParallelGc = option;
                    key = "UseParallelGC";
                } else if (option.matches("^-XX:[\\-+]UseParallelOldGC$")) {
                    // Ignored if CMS previously enabled
                    if (!JdkUtil.isOptionEnabled(useConcMarkSweepGc)) {
                        useParallelOldGc = option;
                    }
                    key = "UseParallelOldGC";
                } else if (option.matches("^-XX:[\\-+]UseParNewGC$")) {
                    useParNewGc = option;
                    key = "UseParNewGC";
                } else if (option.matches("^-XX:[\\-+]UsePerfData$")) {
                    usePerfData = option;
                    key = "UsePerfData";
                } else if (option.matches("^-XX:[\\-+]UseSerialGC$")) {
                    useSerialGc = option;
                    key = "UseSerialGC";
                } else if (option.matches("^-XX:[\\-+]UseShenandoahGC$")) {
                    useShenandoahGc = option;
                    key = "UseShenandoahGC";
                } else if (option.matches("^-XX:[\\-+]UseSplitVerifier$")) {
                    useSplitVerifier = option;
                    key = "UseSplitVerifier";
                } else if (option.matches("^-XX:[\\-+]UseStringDeduplication$")) {
                    useStringDeduplication = option;
                    key = "UseStringDeduplication";
                } else if (option.matches("^-XX:[\\-+]UseThreadPriorities$")) {
                    useThreadPriorities = option;
                    key = "UseThreadPriorities";
                } else if (option.matches("^-XX:[\\-+]UseTLAB$")) {
                    useTlab = option;
                    key = "UseTLAB";
                } else if (option.matches("^-XX:[\\-+]UseVMInterruptibleIO$")) {
                    useVmInterruptibleIo = option;
                    key = "UseVMInterruptibleIO";
                } else if (option.matches("^-XX:[\\-+]UseZGC$")) {
                    useZGc = option;
                    key = "UseZGC";
                } else {
                    undefined.add(option);
                    key = "undefined";
                }
                if (!this.options.containsKey(key)) {
                    this.options.put(key, new ArrayList<String>());
                }
                this.options.get(key).add(option);
            }
        }
        analysis = new ArrayList<Analysis>();
    }

    /**
     * Convenience method to add <code>Analysis</code>.
     * 
     * @param key
     *            The <code>Analysis</code> to check.
     */
    public void addAnalysis(Analysis key) {
        analysis.add(key);
    }

    /**
     * Do JVM options analysis.
     */
    public void doAnalysis() {
        if (jvmContext != null && jvmContext.getOptions() != null && !jvmContext.getOptions().isEmpty()) {
            // Determine collectors based on context (precedence) or JVM options.
            List<GarbageCollector> garbageCollectors = new ArrayList<GarbageCollector>();
            List<GarbageCollector> jvmOptionsGarbageCollectors = getGarbageCollectors();
            if (jvmContext.getGarbageCollectors().size() > 0) {
                garbageCollectors = jvmContext.getGarbageCollectors();
                // Check if collectors are consistent with JVM options
                if (!jvmContext.getGarbageCollectors().contains(GarbageCollector.UNKNOWN)) {
                    if (!jvmOptionsGarbageCollectors.containsAll(garbageCollectors)) {
                        if (getGarbageCollectors().contains(GarbageCollector.G1)
                                && (garbageCollectors.contains(GarbageCollector.PARALLEL_SCAVENGE)
                                        || garbageCollectors.contains(GarbageCollector.PARALLEL_OLD))) {
                            analysis.add(Analysis.ERROR_G1_IGNORED_PARALLEL);
                        } else {
                            analysis.add(Analysis.ERROR_GC_IGNORED);
                        }
                    }
                }
            } else {
                garbageCollectors = jvmOptionsGarbageCollectors;
            }
            // Check for remote debugging enabled
            if (!agentlib.isEmpty()) {
                Iterator<String> iterator = agentlib.iterator();
                Pattern pattern = Pattern.compile("^-agentlib:jdwp=transport=dt_socket.+$");
                while (iterator.hasNext()) {
                    String agentlib = iterator.next();
                    Matcher matcher = pattern.matcher(agentlib);
                    if (matcher.find()) {
                        analysis.add(Analysis.ERROR_REMOTE_DEBUGGING_ENABLED);
                        break;
                    }
                }
            }
            if (!analysis.contains(Analysis.ERROR_REMOTE_DEBUGGING_ENABLED) && !runjdwp.isEmpty()) {
                Iterator<String> iterator = runjdwp.iterator();
                Pattern pattern = Pattern.compile("^-Xrunjdwp:transport=dt_socket.+$");
                while (iterator.hasNext()) {
                    String runjdwp = iterator.next();
                    Matcher matcher = pattern.matcher(runjdwp);
                    if (matcher.find()) {
                        analysis.add(Analysis.ERROR_REMOTE_DEBUGGING_ENABLED);
                        break;
                    }
                }
            }
            if (!undefined.isEmpty()) {
                analysis.add(Analysis.INFO_OPTS_UNDEFINED);
            }
            // Check if initial or max metaspace size being set
            if (metaspaceSize != null || maxMetaspaceSize != null) {
                analysis.add(Analysis.INFO_METASPACE);
            }
            // Check if heap prevented from growing beyond initial heap size
            if (initialHeapSize != null && maxHeapSize != null
                    && (JdkUtil.getByteOptionBytes(JdkUtil.getByteOptionValue(initialHeapSize)) != JdkUtil
                            .getByteOptionBytes(JdkUtil.getByteOptionValue(maxHeapSize)))
                    && JdkUtil.isOptionDisabled(useAdaptiveSizePolicy)) {
                analysis.add(Analysis.WARN_ADAPTIVE_SIZE_POLICY_DISABLED);
            }
            // Check for erroneous perm gen settings
            if (jvmContext.getVersionMajor() >= 8) {
                if (permSize != null) {
                    analysis.add(Analysis.INFO_PERM_SIZE);
                }
                if (maxPermSize != null) {
                    analysis.add(Analysis.INFO_MAX_PERM_SIZE);
                }
            }

            // Check heap dump options
            if (heapDumpOnOutOfMemoryError == null) {
                analysis.add(Analysis.INFO_HEAP_DUMP_ON_OOME_MISSING);
            } else {
                if (JdkUtil.isOptionDisabled(heapDumpOnOutOfMemoryError)) {
                    analysis.add(Analysis.WARN_HEAP_DUMP_ON_OOME_DISABLED);
                } else {
                    if (heapDumpPath == null) {
                        analysis.add(Analysis.INFO_HEAP_DUMP_PATH_MISSING);
                    } else if (heapDumpPath.matches("^.+\\.(hprof|bin)$")) {
                        analysis.add(Analysis.WARN_HEAP_DUMP_PATH_FILENAME);
                    }
                }
            }
            // Check for multi-threaded CMS initial mark disabled
            if (!JdkUtil.isOptionDisabled(useConcMarkSweepGc)
                    && JdkUtil.isOptionDisabled(cmsParallelInitialMarkEnabled)) {
                analysis.add(Analysis.WARN_CMS_PARALLEL_INITIAL_MARK_DISABLED);
            }
            // Check for multi-threaded CMS remark disabled
            if (!JdkUtil.isOptionDisabled(useConcMarkSweepGc) && JdkUtil.isOptionDisabled(cmsParallelRemarkEnabled)) {
                analysis.add(Analysis.WARN_CMS_PARALLEL_REMARK_DISABLED);
            }
            // Compressed object references
            if (jvmContext.getVersionMajor() == JvmContext.UNKNOWN || jvmContext.getVersionMajor() >= 8) {
                if (isCompressedClassPointers()) {
                    analysis.add(Analysis.INFO_METASPACE_CLASS_METADATA_AND_COMP_CLASS_SPACE);
                } else {
                    analysis.add(Analysis.INFO_METASPACE_CLASS_METADATA);
                }
                long thirtyTwoGigabytes = JdkUtil.convertSize(32, 'G', 'B');
                long bytesHeapMaxSize;
                if (maxHeapSize == null && jvmContext.getMemory() > 0) {
                    BigDecimal memory = new BigDecimal(jvmContext.getMemory());
                    memory = memory.divide(new BigDecimal(4));
                    memory = memory.setScale(0, RoundingMode.HALF_EVEN);
                    bytesHeapMaxSize = JdkUtil.convertSize(memory.longValue(), Constants.UNITS, 'B');
                } else {
                    bytesHeapMaxSize = JdkUtil.getByteOptionBytes(maxHeapSize);
                }
                if (bytesHeapMaxSize < thirtyTwoGigabytes) {
                    // Max Heap unknown or < 32g
                    if (!isCompressedOops()) {
                        if (bytesHeapMaxSize < 0) {
                            analysis.add(Analysis.WARN_COMP_OOPS_DISABLED_HEAP_UNK);
                        } else {
                            analysis.add(Analysis.WARN_COMP_OOPS_DISABLED_HEAP_LT_32G);
                        }
                    }
                    if (!isCompressedClassPointers()) {
                        if (bytesHeapMaxSize < 0) {
                            analysis.add(Analysis.WARN_COMP_CLASS_DISABLED_HEAP_UNK);
                        } else {
                            analysis.add(Analysis.WARN_COMP_CLASS_DISABLED_HEAP_LT_32G);
                        }
                    }
                    // Check if MaxMetaspaceSize is less than CompressedClassSpaceSize.
                    if (maxMetaspaceSize != null) {
                        long compressedClassSpaceBytes;
                        if (compressedClassSpaceSize != null) {
                            compressedClassSpaceBytes = JdkUtil
                                    .getByteOptionBytes(JdkUtil.getByteOptionValue(compressedClassSpaceSize));
                        } else {
                            // Default is 1G
                            compressedClassSpaceBytes = JdkUtil.convertSize(1, 'G', 'B');
                        }
                        if (JdkUtil.getByteOptionBytes(
                                JdkUtil.getByteOptionValue(maxMetaspaceSize)) < compressedClassSpaceBytes) {
                            analysis.add(Analysis.WARN_METASPACE_LT_COMP_CLASS);
                        }
                    }
                    // Check for settings being ignored
                    if (JdkUtil.isOptionDisabled(useCompressedOops) && compressedClassSpaceSize != null) {
                        analysis.add(Analysis.INFO_COMP_CLASS_SIZE_COMP_OOPS_DISABLED);
                    }
                    if (JdkUtil.isOptionDisabled(useCompressedClassPointers) && compressedClassSpaceSize != null) {
                        analysis.add(Analysis.INFO_COMP_CLASS_SIZE_COMP_CLASS_DISABLED);
                    }
                } else {
                    // Max Heap >= 32g (should not use compressed pointers)
                    if (JdkUtil.isOptionEnabled(useCompressedOops)) {
                        analysis.add(Analysis.WARN_COMP_OOPS_ENABLED_HEAP_GT_32G);
                    }
                    if (JdkUtil.isOptionEnabled(useCompressedClassPointers)) {
                        analysis.add(Analysis.WARN_COMP_CLASS_ENABLED_HEAP_GT_32G);
                    }
                    // Should not be setting class pointer space size
                    if (compressedClassSpaceSize != null) {
                        analysis.add(Analysis.WARN_COMP_CLASS_SIZE_HEAP_GT_32G);
                    }
                }
            }
            // Check for verbose class loading/unloading logging
            if (verboseClass) {
                analysis.add(Analysis.INFO_VERBOSE_CLASS);
            }
            // Check for -XX:(+|-)TieredCompilation.
            if (JdkUtil.isOptionEnabled(tieredCompilation)) {
                analysis.add(Analysis.INFO_TIERED_COMPILATION_ENABLED);
            } else if (JdkUtil.isOptionDisabled(tieredCompilation)) {
                analysis.add(Analysis.INFO_TIERED_COMPILATION_DISABLED);
            }
            // Check for -XX:-UseBiasedLocking.
            if (JdkUtil.isOptionDisabled(useBiasedLocking) && useShenandoahGc == null) {
                analysis.add(Analysis.WARN_BIASED_LOCKING_DISABLED);
            }
            // PrintGCCause checks
            if (printGcCause != null) {
                if (JdkUtil.isOptionDisabled(printGcCause)) {
                    analysis.add(Analysis.WARN_JDK8_PRINT_GC_CAUSE_DISABLED);
                } else {
                    analysis.add(Analysis.INFO_JDK8_PRINT_GC_CAUSE);
                }
            }
            // Check for -XX:+PrintHeapAtGC.
            if (printHeapAtGc != null) {
                analysis.add(Analysis.INFO_JDK8_PRINT_HEAP_AT_GC);
            }
            // Check for -XX:+PrintTenuringDistribution.
            if (printTenuringDistribution != null) {
                analysis.add(Analysis.INFO_JDK8_PRINT_TENURING_DISTRIBUTION);
            }
            // Check for -XX:PrintFLSStatistics=\\d.
            if (printFLSStatistics != null) {
                analysis.add(Analysis.INFO_JDK8_PRINT_FLS_STATISTICS);
            }
            // Check for experimental options
            if (JdkUtil.isOptionEnabled(unlockExperimentalVmOptions) || !experimental.isEmpty()) {
                // JDK8 < u40 G1 -XX:G1MixedGCLiveThresholdPercent=85 is a valid use case
                if (!((jvmContext.getGarbageCollectors().contains(GarbageCollector.G1) || useG1Gc != null)
                        && jvmContext.getVersionMajor() == 8 && jvmContext.getVersionMinor() < 40
                        && g1MixedGCLiveThresholdPercent != null && experimental.size() == 1
                        && experimental.get(0).equals(g1MixedGCLiveThresholdPercent))) {
                    analysis.add(Analysis.WARN_EXPERIMENTAL_VM_OPTIONS_ENABLED);
                }
            }
            // Check for G1 collector on JDK8 < u40
            if ((jvmContext.getGarbageCollectors().contains(GarbageCollector.G1) || useG1Gc != null)
                    && jvmContext.getVersionMajor() == 8 && jvmContext.getVersionMinor() < 40) {
                analysis.add(Analysis.WARN_JDK8_G1_PRIOR_U40);
                if (g1MixedGCLiveThresholdPercent == null
                        || JdkUtil.getNumberOptionValue(g1MixedGCLiveThresholdPercent) != 85
                        || g1HeapWastePercent == null || JdkUtil.getNumberOptionValue(g1HeapWastePercent) != 5) {
                    analysis.add(Analysis.WARN_JDK8_G1_PRIOR_U40_RECS);
                }
            }

            if (JdkUtil.isOptionEnabled(useCGroupMemoryLimitForHeap)) {
                if (maxHeapSize != null) {
                    analysis.add(Analysis.WARN_CGROUP_MEMORY_LIMIT_OVERRIDE);
                } else {
                    analysis.add(Analysis.WARN_CGROUP_MEMORY_LIMIT);
                }
            }
            if (JdkUtil.isOptionEnabled(useFastUnorderedTimeStamps)) {
                analysis.add(Analysis.WARN_FAST_UNORDERED_TIMESTAMPS);
                if (!analysis.contains(Analysis.WARN_EXPERIMENTAL_VM_OPTIONS_ENABLED)) {
                    analysis.add(Analysis.WARN_EXPERIMENTAL_VM_OPTIONS_ENABLED);
                }
            }
            if (g1MixedGCLiveThresholdPercent != null) {
                analysis.add(Analysis.WARN_G1_MIXED_GC_LIVE_THRSHOLD_PRCNT);
            }
            // Check for diagnostic options
            if (JdkUtil.isOptionEnabled(unlockDiagnosticVmOptions) || !diagnostic.isEmpty()) {
                analysis.add(Analysis.INFO_DIAGNOSTIC_VM_OPTIONS_ENABLED);
            }
            // Check for instrumentation.
            if (!javaagent.isEmpty()) {
                analysis.add(Analysis.INFO_INSTRUMENTATION);
            }
            // If explicit gc is disabled, don't need to set explicit gc options
            if (JdkUtil.isOptionDisabled(explicitGCInvokesConcurrentAndUnloadsClasses)
                    && JdkUtil.isOptionEnabled(disableExplicitGc)) {
                analysis.add(Analysis.INFO_CRUFT_EXP_GC_INV_CON_AND_UNL_CLA);
            }
            // Check for JDK8 gc log file overwrite
            if ((useGcLogFileRotation == null || JdkUtil.isOptionDisabled(useGcLogFileRotation)) && loggc != null
                    && !loggc.contains("%")) {
                analysis.add(Analysis.WARN_JDK8_GC_LOG_FILE_OVERWRITE);
            }
            // Check if JDK8 gc log file rotation missing or disabled
            if (JdkUtil.isOptionDisabled(useGcLogFileRotation)) {
                analysis.add(Analysis.WARN_JDK8_GC_LOG_FILE_ROTATION_DISABLED);
                if (numberOfGcLogFiles != null) {
                    analysis.add(Analysis.WARN_JDK8_GC_LOG_FILE_ROTATION_DISABLED_NUM);
                }
            }
            // JDK11 gc log file rotation checks
            if (!log.isEmpty()) {
                Iterator<String> iterator = log.iterator();
                Pattern pattern = Pattern.compile("^-Xlog:gc(.+)filecount=0.*$");
                while (iterator.hasNext()) {
                    String xLog = iterator.next();
                    Matcher matcher = pattern.matcher(xLog);
                    if (matcher.find()) {
                        analysis.add(Analysis.WARN_JDK11_GC_LOG_FILE_ROTATION_DISABLED);
                        if (!matcher.group(1).contains("%")
                                && !analysis.contains(Analysis.WARN_JDK11_GC_LOG_FILE_OVERWRITE)) {
                            analysis.add(Analysis.WARN_JDK11_GC_LOG_FILE_OVERWRITE);
                        }
                        break;
                    }
                }
            }
            // Check if JDK11 automatic gc log file rotation disabled
            if (!log.isEmpty()) {
                Iterator<String> iterator = log.iterator();
                Pattern pattern = Pattern.compile("^-Xlog:gc(.+)filesize=0.*$");
                while (iterator.hasNext()) {
                    String xLog = iterator.next();
                    Matcher matcher = pattern.matcher(xLog);
                    if (matcher.find()) {
                        analysis.add(Analysis.WARN_JDK11_GC_LOG_FILE_SIZE_0);
                        if (!matcher.group(1).contains("%")
                                && !analysis.contains(Analysis.WARN_JDK11_GC_LOG_FILE_OVERWRITE)) {
                            analysis.add(Analysis.WARN_JDK11_GC_LOG_FILE_OVERWRITE);
                        }
                        break;
                    }
                }
            }
            // Check if JDK8 log file size is small
            if (jvmContext.getVersionMajor() <= 8 && gcLogFileSize != null) {
                BigDecimal fiveGigabytes = new BigDecimal("5").multiply(Constants.MEGABYTE);
                if (JdkUtil.getByteOptionBytes(JdkUtil.getByteOptionValue(gcLogFileSize)) < fiveGigabytes.longValue()) {
                    analysis.add(Analysis.WARN_JDK8_GC_LOG_FILE_SIZE_SMALL);
                }
            }
            // Check if JDK11 log file size is small
            if (!log.isEmpty()) {
                Iterator<String> iterator = log.iterator();
                while (iterator.hasNext()) {
                    String xLog = iterator.next();
                    String filesize = null;
                    Pattern pattern = Pattern.compile("^-Xlog:gc.+filesize=" + JdkRegEx.OPTION_SIZE_BYTES + ".*$");
                    Matcher matcher = pattern.matcher(xLog);
                    if (matcher.find()) {
                        filesize = matcher.group(1);
                    }
                    BigDecimal fiveGigabytes = new BigDecimal("5").multiply(Constants.MEGABYTE);
                    if (JdkUtil.getByteOptionBytes(filesize) < fiveGigabytes.longValue()) {
                        analysis.add(Analysis.WARN_JDK11_GC_LOG_FILE_SIZE_SMALL);
                        break;
                    }
                }
            }
            // Check for JMX enabled
            if (JdkUtil.isOptionEnabled(managementServer)
                    || systemProperties.contains("-Dcom.sun.management.jmxremote")) {
                analysis.add(Analysis.INFO_JMX_ENABLED);
            }
            // Check if native library being used.
            if (!agentlib.isEmpty() || !agentpath.isEmpty()) {
                analysis.add(Analysis.INFO_NATIVE_AGENT);
            }
            // Check for young space >= old space
            if (newSize != null && maxHeapSize != null
                    && JdkMath.calcPercent(JdkUtil.getByteOptionBytes(JdkUtil.getByteOptionValue(newSize)),
                            JdkUtil.getByteOptionBytes(JdkUtil.getByteOptionValue(maxHeapSize))) >= 50) {
                analysis.add(Analysis.INFO_NEW_RATIO_INVERTED);
            }
            // Check for -XX:-PrintAdaptiveSizePolicy / -XX:+PrintAdaptiveSizePolicy
            if (JdkUtil.isOptionDisabled(printAdaptiveSizePolicy)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_ADAPTIVE_RESIZE_PLCY_DISABLED);
            } else if (JdkUtil.isOptionEnabled(printAdaptiveSizePolicy)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_ADAPTIVE_RESIZE_PLCY_ENABLED);
            }
            // Check for -XX:+PrintPromotionFailure option being used
            if (JdkUtil.isOptionEnabled(printPromotionFailure)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_PROMOTION_FAILURE);
            }
            // Check if background compilation disabled.
            if (batch || JdkUtil.isOptionDisabled(backgroundCompilation)) {
                analysis.add(Analysis.WARN_BYTECODE_BACK_COMP_DISABLED);
            }
            // Check if just in time (JIT) compilation disabled.
            if (xInt) {
                analysis.add(Analysis.WARN_BYTECODE_COMPILE_DISABLED);
            }
            // Check if compilation being forced on first invocation.
            if (comp) {
                analysis.add(Analysis.WARN_BYTECODE_COMPILE_FIRST_INVOCATION);
            }
            // Check for class unloading disabled
            if (JdkUtil.isOptionDisabled(classUnloading)) {
                analysis.add(Analysis.WARN_CLASS_UNLOADING_DISABLED);
            }
            // Check if CMS handling metaspace collections is disabled or not enabled
            if (!JdkUtil.isOptionDisabled(useConcMarkSweepGc) && JdkUtil.isOptionDisabled(cmsClassUnloadingEnabled)) {
                analysis.add(Analysis.WARN_CMS_CLASS_UNLOADING_DISABLED);
            }
            // Check for incremental mode in combination with
            // -XX:CMSInitiatingOccupancyFraction=<n>.
            if (!JdkUtil.isOptionDisabled(useConcMarkSweepGc) && JdkUtil.isOptionEnabled(cmsIncrementalMode)
                    && cmsInitiatingOccupancyFraction != null) {
                analysis.add(Analysis.WARN_CMS_INC_MODE_WITH_INIT_OCCUP_FRACT);
            }
            // Check for-XX:CMSInitiatingOccupancyFraction without
            // -XX:+UseCMSInitiatingOccupancyOnly.
            if (!JdkUtil.isOptionDisabled(useConcMarkSweepGc) && cmsInitiatingOccupancyFraction != null
                    && !JdkUtil.isOptionEnabled(useCmsInitiatingOccupancyOnly)) {
                analysis.add(Analysis.INFO_CMS_INIT_OCCUPANCY_ONLY_MISSING);
            }
            // Check PAR_NEW disabled, redundant, or cruft
            if (JdkUtil.isOptionEnabled(useConcMarkSweepGc)) {
                if (JdkUtil.isOptionDisabled(useParNewGc)) {
                    analysis.add(Analysis.ERROR_JDK8_CMS_PAR_NEW_DISABLED);
                } else if (JdkUtil.isOptionEnabled(useParNewGc)) {
                    analysis.add(Analysis.INFO_JDK8_CMS_PAR_NEW_REDUNDANT);
                }
            } else if (JdkUtil.isOptionDisabled(useConcMarkSweepGc)) {
                analysis.add(Analysis.INFO_CMS_DISABLED);
            } else if (useParNewGc != null && !JdkUtil.isOptionDisabled(useParallelOldGc)) {
                analysis.add(Analysis.INFO_JDK8_CMS_PAR_NEW_CRUFT);
            }
            // Check PARALLEL_OLD disabled, redundant, or cruft
            if (JdkUtil.isOptionEnabled(useParallelGc) || (isDefaultCollector() && jvmContext.getVersionMajor() >= 7
                    && jvmContext.getVersionMajor() <= 8)) {
                // Parallel collector is explicitly enabled, or JDK8 with no collector specified
                if (JdkUtil.isOptionDisabled(useParallelOldGc)) {
                    analysis.add(Analysis.ERROR_PARALLEL_SCAVENGE_PARALLEL_SERIAL_OLD);
                } else if (JdkUtil.isOptionEnabled(useParallelOldGc)) {
                    analysis.add(Analysis.INFO_PARALLEL_OLD_REDUNDANT);
                }
            } else if (useParallelOldGc == null && options.containsKey("UseParallelOldGC")) {
                // -XX:(+|-)UseParallelOldGC is being overriden (e.g. by -XX:+UseConcMarkSweepGC)
                analysis.add(Analysis.INFO_PARALLEL_OLD_CRUFT);
            }
            // Check to see if explicit gc is disabled
            if (JdkUtil.isOptionEnabled(disableExplicitGc)) {
                analysis.add(Analysis.WARN_EXPLICIT_GC_DISABLED);
                // Specifying that explicit gc being collected concurrently makes no sense if
                // explicit gc is disabled.
                if (JdkUtil.isOptionEnabled(explicitGCInvokesConcurrent)) {
                    analysis.add(Analysis.WARN_EXPLICIT_GC_DISABLED_CONCURRENT);
                }
            }
            // Check for outputting application concurrent time
            if (JdkUtil.isOptionEnabled(printGcApplicationConcurrentTime)) {
                analysis.add(Analysis.INFO_PRINT_GC_APPLICATION_CONCURRENT_TIME);

            }
            // Check for print class histogram output enabled with -XX:+PrintClassHistogram,
            // -XX:+PrintClassHistogramBeforeFullGC, or -XX:+PrintClassHistogramAfterFullGC.
            if (JdkUtil.isOptionEnabled(printClassHistogram)) {
                analysis.add(Analysis.WARN_PRINT_CLASS_HISTOGRAM);
            }
            if (JdkUtil.isOptionEnabled(printClassHistogramAfterFullGc)) {
                analysis.add(Analysis.WARN_PRINT_CLASS_HISTOGRAM_AFTER_FULL_GC);
            }
            if (JdkUtil.isOptionEnabled(printClassHistogramBeforeFullGc)) {
                analysis.add(Analysis.WARN_PRINT_CLASS_HISTOGRAM_BEFORE_FULL_GC);
            }
            // Check for tenuring disabled or default overriden
            long tenuring = JdkUtil.getNumberOptionValue(maxTenuringThreshold);
            if (tenuring == 0) {
                analysis.add(Analysis.WARN_TENURING_DISABLED);
            } else if (tenuring > 0 && tenuring < 15) {
                analysis.add(Analysis.INFO_MAX_TENURING_OVERRIDE);
            }
            // Check for -XX:+UseMembar option being used
            if (JdkUtil.isOptionEnabled(useMembar)) {
                analysis.add(Analysis.WARN_USE_MEMBAR);
            }
            // Check for setting DGC intervals when explicit GC is disabled.
            if (JdkUtil.isOptionEnabled(disableExplicitGc)) {
                if (getSunRmiDgcClientGcInterval() != null) {
                    analysis.add(Analysis.INFO_RMI_DGC_CLIENT_GCINTERVAL_REDUNDANT);
                }
                if (getSunRmiDgcServerGcInterval() != null) {
                    analysis.add(Analysis.INFO_RMI_DGC_SERVER_GCINTERVAL_REDUNDANT);
                }
            }
            // Check for small DGC intervals.
            if (getSunRmiDgcClientGcInterval() != null && !JdkUtil.isOptionEnabled(disableExplicitGc)) {
                long sunRmiDgcClientGcInterval = JdkUtil.getNumberOptionValue(getSunRmiDgcClientGcInterval());
                if (sunRmiDgcClientGcInterval < 3600000) {
                    analysis.add(Analysis.WARN_RMI_DGC_CLIENT_GCINTERVAL_SMALL);
                } else if (sunRmiDgcClientGcInterval > 86400000) {
                    analysis.add(Analysis.WARN_RMI_DGC_CLIENT_GCINTERVAL_LARGE);
                }
            }
            if (getSunRmiDgcServerGcInterval() != null && !JdkUtil.isOptionEnabled(disableExplicitGc)) {
                long sunRmiDgcServerGcInterval = JdkUtil.getNumberOptionValue(getSunRmiDgcServerGcInterval());
                if (sunRmiDgcServerGcInterval < 3600000) {
                    analysis.add(Analysis.WARN_RMI_DGC_SERVER_GCINTERVAL_SMALL);
                } else if (sunRmiDgcServerGcInterval > 86400000) {
                    analysis.add(Analysis.WARN_RMI_DGC_SERVER_GCINTERVAL_LARGE);
                }
            }
            // Check for -XX:+PrintReferenceGC.
            if (JdkUtil.isOptionEnabled(printReferenceGc)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_REFERENCE_GC_ENABLED);
            }
            // Check for -XX:+PrintStringDeduplicationStatistics.
            if (JdkUtil.isOptionEnabled(printStringDeduplicationStatistics)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_STRING_DEDUP_STATS_ENABLED);
            }
            // Check for -XX:+PrintStringTableStatistics.
            if (JdkUtil.isOptionEnabled(printStringTableStatistics)) {
                analysis.add(Analysis.INFO_JDK8_PRINT_STRING_TABLE_STATS_ENABLED);
            }
            // Check for trace class loading enabled with -XX:+TraceClassLoading
            if (JdkUtil.isOptionEnabled(traceClassLoading)) {
                analysis.add(Analysis.INFO_TRACE_CLASS_LOADING);
            }
            // Check for trace class unloading enabled with -XX:+TraceClassUnloading
            if (JdkUtil.isOptionEnabled(traceClassUnloading)) {
                analysis.add(Analysis.INFO_TRACE_CLASS_UNLOADING);
            }
            // Check for -XX:SurvivorRatio option being used
            if (survivorRatio != null) {
                analysis.add(Analysis.INFO_SURVIVOR_RATIO);
            }
            // Check for -XX:TargetSurvivorRatio option being used
            if (targetSurvivorRatio != null) {
                analysis.add(Analysis.INFO_SURVIVOR_RATIO_TARGET);
            }
            // Check for JFR being used
            if (flightRecorderOptions != null) {
                analysis.add(Analysis.INFO_JFR);
            }
            // Check for -XX:+EliminateLocks
            if (eliminateLocks != null && JdkUtil.isOptionEnabled(eliminateLocks)) {
                analysis.add(Analysis.INFO_ELIMINATE_LOCKS_ENABLED);
            }
            // Check for -XX:-UseVMInterruptibleIO
            if (useVmInterruptibleIo != null) {
                analysis.add(Analysis.WARN_JDK8_USE_VM_INTERRUPTIBLE_IO);
            }
            // Check for class verification disabled
            if (verify != null && verify.equals("-Xverify:none")) {
                analysis.add(Analysis.WARN_VERIFY_NONE);
            }
            // Check for max heap size not being explicitly set
            if (maxHeapSize == null) {
                analysis.add(Analysis.INFO_HEAP_MAX_MISSING);
            }
            // Check if JVM signal handling disabled
            if (rs) {
                analysis.add(Analysis.WARN_RS);
            }
            // Check JDK8 gc log file rotation
            if (jvmContext.getVersionMajor() <= 8 && loggc != null && useGcLogFileRotation == null) {
                analysis.add(Analysis.WARN_JDK8_GC_LOG_FILE_ROTATION_NOT_ENABLED);
            }
            // Check if gc logging is being sent to stdout
            if (isGcLoggingToStdout()) {
                analysis.add(Analysis.INFO_GC_LOG_STDOUT);
            }
            // Check for the creation of the AttachListener socket file
            // (/tmp/.java_pid<pid>) disabled
            if (JdkUtil.isOptionEnabled(disableAttachMechanism)) {
                analysis.add(Analysis.WARN_DISABLE_ATTACH_MECHANISM);
            }
            // Check for ignored -XX:CompileThreshold
            if (!JdkUtil.isOptionDisabled(tieredCompilation) && compileThreshold != null) {
                analysis.add(Analysis.INFO_COMPILE_THRESHOLD_IGNORED);
            }
            // Check for parallel class loading -XX:+UnsyncloadClass
            if (JdkUtil.isOptionEnabled(unsyncloadClass)) {
                analysis.add(Analysis.WARN_DIAGNOSTIC_UNSYNCLOAD_CLASS);
            }
            // Check for guaranteed safepoint interval being set
            if (guaranteedSafepointInterval != null) {
                analysis.add(Analysis.WARN_DIAGNOSTICS_GUARANTEED_SAFEPOINT_INTERVAL);
            }
            // Check for safepoint logging
            if (JdkUtil.isOptionEnabled(printSafepointStatistics)) {
                analysis.add(Analysis.WARN_DIAGNOSTIC_PRINT_SAFEPOINT_STATISTICS);
            }
            // Check for non safepoint debugging is enabled
            if (JdkUtil.isOptionEnabled(debugNonSafepoints)) {
                analysis.add(Analysis.WARN_DIAGNOSTIC_DEBUG_NON_SAFEPOINTS);
            }
            // Check ParallelGCThreads
            if (parallelGcThreads != null) {
                if (JdkUtil.isOptionEnabled(useSerialGc)) {
                    analysis.add(Analysis.INFO_PARALLEL_GC_THREADS_SERIAL);
                } else if (JdkUtil.getNumberOptionValue(parallelGcThreads) == 1) {
                    analysis.add(Analysis.ERROR_PARALLEL_GC_THREADS_1);
                } else {
                    analysis.add(Analysis.INFO_PARALLEL_GC_THREADS);
                }
            }
            if (ciCompilerCount != null) {
                analysis.add(Analysis.INFO_CI_COMPILER_COUNT);
            }
            // Check for -XX:MinHeapDeltaBytes=N
            if (minHeapDeltaBytes != null) {
                analysis.add(Analysis.INFO_MIN_HEAP_DELTA_BYTES);
            }
            // Check for -Xdebug
            if (debug) {
                analysis.add(Analysis.INFO_DEBUG);
            }
            // Check for deprecated JDK8 logging options on JDK11+
            if (jvmContext.getVersionMajor() >= 9) {
                if (loggc != null) {
                    analysis.add(Analysis.INFO_JDK9_DEPRECATED_LOGGC);
                }
                if (printGc != null) {
                    analysis.add(Analysis.INFO_JDK9_DEPRECATED_PRINT_GC);
                }
                if (printGcDetails != null) {
                    analysis.add(Analysis.INFO_JDK9_DEPRECATED_PRINT_GC_DETAILS);
                }
            }
            // Check for -Xconcurrentio
            if (concurrentio) {
                analysis.add(Analysis.WARN_CONCURRENTIO);
            }
            // Check if summarized remembered set processing information being output
            if (garbageCollectors.contains(GarbageCollector.G1) && JdkUtil.isOptionEnabled(g1SummarizeRSetStats)
                    && JdkUtil.getNumberOptionValue(g1SummarizeRSetStatsPeriod) > 0) {
                analysis.add(Analysis.INFO_G1_SUMMARIZE_RSET_STATS_OUTPUT);
            }
            // Check OnOutOfMemoryError
            if (onOutOfMemoryError != null) {
                if (onOutOfMemoryError.matches("^.+kill -9.+$") && (jvmContext.getVersionMajor() > 8
                        || (jvmContext.getVersionMajor() == 8 && jvmContext.getVersionMinor() >= 92))) {
                    analysis.add(Analysis.INFO_ON_OOME_KILL);
                } else {
                    analysis.add(Analysis.INFO_ON_OOME);
                }
            }
            // If CMS or G1, explicit gc is not handled concurrently by default
            if ((garbageCollectors.contains(GarbageCollector.CMS) || garbageCollectors.contains(GarbageCollector.G1))
                    && !JdkUtil.isOptionEnabled(explicitGCInvokesConcurrent)
                    && !JdkUtil.isOptionEnabled(disableExplicitGc)) {
                analysis.add(Analysis.WARN_EXPLICIT_GC_NOT_CONCURRENT);
            }
            // Check for redundant -server flag and ignored -client flag on 64-bit
            if (jvmContext.getBit() != Bit.BIT32) {
                if (isD64()) {
                    analysis.add(Analysis.INFO_64_D64_REDUNDANT);
                }
                if (isServer()) {
                    analysis.add(Analysis.INFO_64_SERVER_REDUNDANT);
                }
                if (isClient()) {
                    analysis.add(Analysis.INFO_64_CLIENT);
                }
            }
            if (jvmContext.isContainer() && !JdkUtil.isOptionDisabled(usePerfData)
            // Check if performance data is being written to disk in a container environment
                    && !JdkUtil.isOptionEnabled(perfDisableSharedMem)) {
                analysis.add(Analysis.WARN_CONTAINER_PERF_DATA_DISK);
            }
            // Check if performance data disabled
            if (JdkUtil.isOptionDisabled(usePerfData)) {
                analysis.add(Analysis.INFO_PERF_DATA_DISABLED);
            }
            // Check if print gc details option disabled
            if (jvmContext.getVersionMajor() <= 8) {
                if (printGcDetails == null && isGcLoggingEnable()) {
                    analysis.add(Analysis.WARN_JDK8_PRINT_GC_DETAILS_MISSING);
                } else if (JdkUtil.isOptionDisabled(printGcDetails)) {
                    analysis.add(Analysis.WARN_JDK8_PRINT_GC_DETAILS_DISABLED);
                }
            }
            // Check JDK11 print gc details option missing
            if (jvmContext.getVersionMajor() == 11 && !log.isEmpty()) {
                Iterator<String> iterator = log.iterator();
                boolean haveGcDetails = false;
                while (iterator.hasNext()) {
                    String xLog = iterator.next();
                    // negative lookahead: matches "gc*" followed by anything other than "=off"
                    if (xLog.matches("^.+gc\\*(?!=off).+$")) {
                        haveGcDetails = true;
                        break;
                    }
                }
                if (!haveGcDetails) {
                    analysis.add(Analysis.INFO_JDK11_PRINT_GC_DETAILS_MISSING);
                }
            }
            // Check heap initial/max values non container
            if (!jvmContext.isContainer() && initialHeapSize != null && maxHeapSize != null
                    && (JdkUtil.getByteOptionBytes(JdkUtil.getByteOptionValue(initialHeapSize)) != JdkUtil
                            .getByteOptionBytes(JdkUtil.getByteOptionValue(maxHeapSize)))) {
                analysis.add(Analysis.INFO_HEAP_MIN_NOT_EQUAL_MAX);
            }
            // Test extraneous use of -XX:LargePageSizeInBytes
            if (largePageSizeInBytes != null) {
                if (jvmContext.getOs() == Os.LINUX) {
                    analysis.add(Analysis.INFO_LARGE_PAGE_SIZE_IN_BYTES_LINUX);
                } else if (jvmContext.getOs() == Os.WINDOWS) {
                    analysis.add(Analysis.INFO_LARGE_PAGE_SIZE_IN_BYTES_WINDOWS);
                }
            }
            // Thread stack size
            if (threadStackSize == null) {
                if (jvmContext.getBit() == Bit.BIT32) {
                    analysis.add(Analysis.WARN_THREAD_STACK_SIZE_NOT_SET_32);
                }
            } else {
                char fromUnits;
                long value;
                Pattern pattern = Pattern.compile("^-(X)?(ss|X:ThreadStackSize=)" + JdkRegEx.OPTION_SIZE_BYTES + "$");
                Matcher matcher = pattern.matcher(threadStackSize);
                if (matcher.find()) {
                    value = Long.parseLong(matcher.group(4));
                    if (matcher.group(2) != null && matcher.group(2).equals("X:ThreadStackSize=")) {
                        // value is in kilobytes, multiply by 1024
                        value = JdkUtil.convertSize(value, 'K', 'B');
                    }
                    if (matcher.group(5) != null) {
                        fromUnits = matcher.group(5).charAt(0);
                    } else {
                        fromUnits = 'B';
                    }
                    long threadStackMaxSize = JdkUtil.convertSize(value, fromUnits, 'K');
                    if (threadStackMaxSize < 1) {
                        analysis.add(Analysis.WARN_THREAD_STACK_SIZE_TINY);
                    } else if (threadStackMaxSize < 128) {
                        analysis.add(Analysis.WARN_THREAD_STACK_SIZE_SMALL);
                    } else if (threadStackMaxSize > 1024) {
                        analysis.add(Analysis.WARN_THREAD_STACK_SIZE_LARGE);
                    }
                }
            }
            // Java Thread API
            if (useThreadPriorities == null || JdkUtil.isOptionEnabled(useThreadPriorities)) {
                if (JdkUtil.isOptionEnabled(useThreadPriorities)) {
                    analysis.add(Analysis.INFO_USE_THREAD_PRIORITIES_REDUNDANT);
                }
                if (threadPriorityPolicy != null) {
                    long policy = JdkUtil.getNumberOptionValue(threadPriorityPolicy);
                    if (policy < 0) {
                        analysis.add(Analysis.WARN_THREAD_PRIORITY_POLICY_BAD);
                    } else if (policy == 0) {
                        analysis.add(Analysis.INFO_THREAD_PRIORITY_POLICY_REDUNDANT);
                    } else if (policy == 1) {
                        analysis.add(Analysis.WARN_THREAD_PRIORITY_POLICY_AGGRESSIVE);
                    } else if (policy > 1) {
                        analysis.add(Analysis.WARN_THREAD_PRIORITY_POLICY_AGGRESSIVE_BACKDOOR);
                    }
                }
            } else if (JdkUtil.isOptionDisabled(useThreadPriorities)) {
                analysis.add(Analysis.WARN_USE_THREAD_PRIORITIES_DISABLED);
                if (threadPriorityPolicy != null) {
                    analysis.add(Analysis.WARN_THREAD_PRIORITY_POLICY_IGNORED);
                }
            }
            // Non-standard CMS options
            if (cmsWaitDuration != null) {
                analysis.add(Analysis.INFO_CMS_WAIT_DURATION);
            }
            if (cmsEdenChunksRecordAlways != null) {
                analysis.add(Analysis.INFO_CMS_EDEN_CHUNK_RECORD_ALWAYS);
            }
            // -XX:+UseCondCardMark
            if (JdkUtil.isOptionEnabled(useCondCardMark)) {
                analysis.add(Analysis.WARN_USE_COND_CARD_MARK);
            }
            // Check for unaccounted disabled options
            if (getUnaccountedDisabledOptions() != null) {
                analysis.add(Analysis.INFO_UNACCOUNTED_OPTIONS_DISABLED);
            }
            // Check if CMS not being used for old collections
            if (JdkUtil.isOptionEnabled(useParNewGc)) {
                if (useConcMarkSweepGc == null && useParallelOldGc == null) {
                    analysis.add(Analysis.ERROR_CMS_MISSING);
                } else if (JdkUtil.isOptionDisabled(useParallelOldGc)) {
                    analysis.add(Analysis.ERROR_PAR_NEW_SERIAL_OLD);
                }
            }
            // Duplicate options
            if (getDuplicates() != null) {
                analysis.add(Analysis.ERROR_DUPS);
            }
            // Check for -XX:+MaxFDLimit being ignored
            if (jvmContext.getOs() != Os.UNIDENTIFIED && jvmContext.getOs() != Os.SOLARIS && maxFdLimit != null) {
                analysis.add(Analysis.INFO_MAX_FD_LIMIT_IGNORED);
            }
            // Check for -XX:-UseGCOverheadLimit.
            if (JdkUtil.isOptionDisabled(useGcOverheadLimit)) {
                analysis.add(Analysis.INFO_GC_OVERHEAD_LIMIT_DISABLED);
            }
            // Check for -XX:+IgnoreUnrecognizedVMOptions.
            if (JdkUtil.isOptionEnabled(ignoreUnrecognizedVmOptions)) {
                analysis.add(Analysis.INFO_IGNORE_UNRECOGNIZED_VM_OPTIONS);
            }
            // Check for -XX:+UseCMSCompactAtFullCollection.
            if (JdkUtil.isOptionEnabled(useCmsCompactAtFullCollection)) {
                analysis.add(Analysis.ERROR_JDK8_USE_CMS_COMPACTION_AT_FULL_GC_ENABLED);
            }
            // Check for JNI validation enabled
            if (isCheckJni()) {
                analysis.add(Analysis.WARN_CHECK_JNI_ENABLED);
            }
        }
    }

    public String getActiveProcessorCount() {
        return activeProcessorCount;
    }

    public String getAdaptiveSizePolicyWeight() {
        return adaptiveSizePolicyWeight;
    }

    public ArrayList<String> getAddExports() {
        return addExports;
    }

    public ArrayList<String> getAddModules() {
        return addModules;
    }

    public ArrayList<String> getAddOpens() {
        return addOpens;
    }

    public ArrayList<String> getAgentlib() {
        return agentlib;
    }

    public ArrayList<String> getAgentpath() {
        return agentpath;
    }

    public String getAggressiveHeap() {
        return aggressiveHeap;
    }

    public String getAggressiveOpts() {
        return aggressiveOpts;
    }

    public String getAlwaysPreTouch() {
        return alwaysPreTouch;
    }

    /**
     * @return Analysis as a <code>List</code> of String arrays with 2 elements, the first the key, the second the
     *         display literal.
     */
    public List<String[]> getAnalysis() {
        List<String[]> a = new ArrayList<String[]>();
        Iterator<Analysis> itJvmOptionsAnalysis = analysis.iterator();
        while (itJvmOptionsAnalysis.hasNext()) {
            Analysis item = itJvmOptionsAnalysis.next();
            if (item.getKey().equals(Analysis.ERROR_DUPS.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                s.append(getDuplicates());
                s.append(".");
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.INFO_DIAGNOSTIC_VM_OPTIONS_ENABLED.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                Iterator<String> iterator = getDiagnostic().iterator();
                while (iterator.hasNext()) {
                    String diagnostic = iterator.next();
                    s.append(" ");
                    s.append(diagnostic);
                }
                s.append(".");
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.INFO_OPTS_UNDEFINED.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                Iterator<String> iterator = getUndefined().iterator();
                while (iterator.hasNext()) {
                    String undefined = iterator.next();
                    s.append(" ");
                    s.append(undefined);
                }
                s.append(".");
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.INFO_UNACCOUNTED_OPTIONS_DISABLED.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                s.append(getUnaccountedDisabledOptions());
                s.append(".");
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.INFO_METASPACE_CLASS_METADATA_AND_COMP_CLASS_SPACE.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                long bytesMaxMetaspaceSize = JdkUtil.getByteOptionBytes(maxMetaspaceSize);
                long bytesCompressedClassSpaceSize;
                if (hasAnalysis(Analysis.WARN_METASPACE_LT_COMP_CLASS.getKey())) {
                    long bytesInitialBootClassLoaderMetaspaceSize;
                    if (initialBootClassLoaderMetaspaceSize == null) {
                        BigDecimal fourMegabytes = new BigDecimal("4").multiply(Constants.MEGABYTE);
                        bytesInitialBootClassLoaderMetaspaceSize = fourMegabytes.longValue();
                    } else {
                        bytesInitialBootClassLoaderMetaspaceSize = JdkUtil
                                .getByteOptionBytes(initialBootClassLoaderMetaspaceSize);
                    }
                    bytesCompressedClassSpaceSize = bytesMaxMetaspaceSize
                            - (2 * bytesInitialBootClassLoaderMetaspaceSize);
                } else {
                    if (compressedClassSpaceSize != null) {
                        bytesCompressedClassSpaceSize = JdkUtil.getByteOptionBytes(compressedClassSpaceSize);
                    } else {
                        bytesCompressedClassSpaceSize = JdkUtil.convertSize(1, 'G', 'B');
                    }
                }
                String replace = "Metaspace = Class Metadata + Compressed Class Space";
                int position = s.toString().lastIndexOf(replace);
                StringBuffer with = new StringBuffer("Metaspace(");
                if (bytesMaxMetaspaceSize == Constants.UNKNOWN) {
                    with.append("unlimited");
                } else {
                    with.append(JdkUtil.convertSize(bytesMaxMetaspaceSize, 'B', Constants.UNITS));
                    with.append(Constants.UNITS);
                }
                with.append(") = Class Metadata(");
                if (bytesMaxMetaspaceSize == Constants.UNKNOWN) {
                    with.append("unlimited");
                } else {
                    with.append(JdkUtil.convertSize(bytesMaxMetaspaceSize - bytesCompressedClassSpaceSize, 'B',
                            Constants.UNITS));
                    with.append(Constants.UNITS);
                }
                with.append(") + Compressed Class Space(");
                with.append(JdkUtil.convertSize((bytesCompressedClassSpaceSize), 'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(")");
                s.replace(position, position + replace.length(), with.toString());
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.WARN_EXPERIMENTAL_VM_OPTIONS_ENABLED.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                Iterator<String> iterator = getExperimental().iterator();
                while (iterator.hasNext()) {
                    String experimental = iterator.next();
                    s.append(" ");
                    s.append(experimental);
                }
                s.append(".");
                a.add(new String[] { item.getKey(), s.toString() });
            } else if (item.getKey().equals(Analysis.WARN_METASPACE_LT_COMP_CLASS.toString())) {
                StringBuffer s = new StringBuffer(item.getValue());
                long bytesMaxMetaspaceSize = JdkUtil.getByteOptionBytes(maxMetaspaceSize);
                long bytesInitialBootClassLoaderMetaspaceSize;
                if (initialBootClassLoaderMetaspaceSize == null) {
                    bytesInitialBootClassLoaderMetaspaceSize = JdkUtil.convertSize(4, 'M', 'B');
                } else {
                    bytesInitialBootClassLoaderMetaspaceSize = JdkUtil
                            .getByteOptionBytes(initialBootClassLoaderMetaspaceSize);
                }
                String replace = "CompressedClassSpaceSize' = MaxMetaspaceSize - [2 * "
                        + "InitialBootClassLoaderMetaspaceSize]. Class Metadata Size' = MaxMetaspaceSize - "
                        + "CompressedClassSpaceSize'";
                int position = s.toString().lastIndexOf(replace);
                StringBuffer with = new StringBuffer("CompressedClassSpaceSize' = MaxMetaspaceSize(");
                with.append(JdkUtil.convertSize(bytesMaxMetaspaceSize, 'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(") - [2 * InitialBootClassLoaderMetaspaceSize(");
                with.append(JdkUtil.convertSize(bytesInitialBootClassLoaderMetaspaceSize, 'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(")] = ");
                with.append(
                        JdkUtil.convertSize((bytesMaxMetaspaceSize - (2 * bytesInitialBootClassLoaderMetaspaceSize)),
                                'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(". Class Metadata Size' = MaxMetaspaceSize(");
                with.append(JdkUtil.convertSize(bytesMaxMetaspaceSize, 'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(") - CompressedClassSpaceSize'(");
                with.append(
                        JdkUtil.convertSize((bytesMaxMetaspaceSize - (2 * bytesInitialBootClassLoaderMetaspaceSize)),
                                'B', Constants.UNITS));
                with.append(Constants.UNITS);
                with.append(") = ");
                with.append(JdkUtil.convertSize((2 * bytesInitialBootClassLoaderMetaspaceSize), 'B', Constants.UNITS));
                with.append(Constants.UNITS);
                s.replace(position, position + replace.length(), with.toString());
                a.add(new String[] { item.getKey(), s.toString() });
            } else {
                a.add(new String[] { item.getKey(), item.getValue() });
            }
        }
        return a;
    }

    /**
     * Convenience method to get the <code>Analysis</code> literal.
     * 
     * @param key
     *            The <code>Analysis</code> key.
     * @return The <code>Analysis</code> display literal, or null if it does not exist.
     */
    public String getAnalysisLiteral(String key) {
        String literal = null;
        Iterator<String[]> i = getAnalysis().iterator();
        while (i.hasNext()) {
            String[] item = i.next();
            if (item[0].equals(key)) {
                literal = item[1];
                break;
            }
        }
        return literal;
    }

    public String getAutoBoxCacheMax() {
        return autoBoxCacheMax;
    }

    public String getBackgroundCompilation() {
        return backgroundCompilation;
    }

    public ArrayList<String> getBootclasspath() {
        return bootclasspath;
    }

    public String getCiCompilerCount() {
        return ciCompilerCount;
    }

    public String getClasspath() {
        return classpath;
    }

    public String getClassUnloading() {
        return classUnloading;
    }

    public String getCmsClassUnloadingEnabled() {
        return cmsClassUnloadingEnabled;
    }

    public String getCmsEdenChunksRecordAlways() {
        return cmsEdenChunksRecordAlways;
    }

    public String getCmsIncrementalMode() {
        return cmsIncrementalMode;
    }

    public String getCmsIncrementalSafetyFactor() {
        return cmsIncrementalSafetyFactor;
    }

    public String getCmsInitiatingOccupancyFraction() {
        return cmsInitiatingOccupancyFraction;
    }

    public String getCmsParallelInitialMarkEnabled() {
        return cmsParallelInitialMarkEnabled;
    }

    public String getCmsParallelRemarkEnabled() {
        return cmsParallelRemarkEnabled;
    }

    public String getCmsScavengeBeforeRemark() {
        return cmsScavengeBeforeRemark;
    }

    public String getCmsWaitDuration() {
        return cmsWaitDuration;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public String getCompileCommandFile() {
        return compileCommandFile;
    }

    public String getCompileThreshold() {
        return compileThreshold;
    }

    public String getCompressedClassSpaceSize() {
        return compressedClassSpaceSize;
    }

    public String getConcGcThreads() {
        return concGcThreads;
    }

    public String getCrashOnOutOfMemoryError() {
        return crashOnOutOfMemoryError;
    }

    public String getDebugNonSafepoints() {
        return debugNonSafepoints;
    }

    public ArrayList<String> getDiagnostic() {
        return diagnostic;
    }

    public String getDisableAttachMechanism() {
        return disableAttachMechanism;
    }

    /**
     * Disabled JVM options.
     * 
     * @return the disabled JVM options.
     */
    public ArrayList<String> getDisabledOptions() {
        ArrayList<String> disabledOptions = new ArrayList<String>();
        if (jvmContext.getOptions() != null) {
            String regex = "(-XX:-[\\S]+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(jvmContext.getOptions());
            while (matcher.find()) {
                disabledOptions.add(matcher.group(1));
            }
        }
        return disabledOptions;
    }

    public String getDisableExplicitGc() {
        return disableExplicitGc;
    }

    public String getDoEscapeAnalysis() {
        return doEscapeAnalysis;
    }

    /**
     * Duplicate JVM options.
     * 
     * @return The duplicate JVM options, or null if no duplicates.
     */
    public String getDuplicates() {
        String duplicates = null;
        if (options != null) {
            Iterator<Entry<String, ArrayList<String>>> iteratorOptions = getOptions().entrySet().iterator();
            StringBuffer options = new StringBuffer();
            boolean firstEntry = true;
            while (iteratorOptions.hasNext()) {
                Entry<String, ArrayList<String>> option = iteratorOptions.next();
                if (!option.getKey().equals("D") && !option.getKey().equals("undefined")
                        && !option.getKey().equals("Xbootclasspath") && option.getValue().size() > 1) {
                    ArrayList<String> opt = option.getValue();
                    Iterator<String> iteratorOption = opt.iterator();
                    while (iteratorOption.hasNext()) {
                        if (!firstEntry) {
                            options.append(" ");
                        }
                        options.append(iteratorOption.next());
                        firstEntry = false;
                    }
                }
            }
            if (options.length() > 0) {
                duplicates = options.toString();
            }
        }
        return duplicates;
    }

    public String getEliminateLocks() {
        return eliminateLocks;
    }

    public String getErrorFile() {
        return errorFile;
    }

    public String getExitOnOutOfMemoryError() {
        return exitOnOutOfMemoryError;
    }

    public ArrayList<String> getExperimental() {
        return experimental;
    }

    public String getExplicitGCInvokesConcurrent() {
        return explicitGCInvokesConcurrent;
    }

    public String getExplicitGCInvokesConcurrentAndUnloadsClasses() {
        return explicitGCInvokesConcurrentAndUnloadsClasses;
    }

    public String getExtensiveErrorReports() {
        return extensiveErrorReports;
    }

    public String getFlightRecorderOptions() {
        return flightRecorderOptions;
    }

    public String getG1ConcRefinementThreads() {
        return g1ConcRefinementThreads;
    }

    public String getG1HeapRegionSize() {
        return g1HeapRegionSize;
    }

    public String getG1HeapWastePercent() {
        return g1HeapWastePercent;
    }

    public String getG1MaxNewSizePercent() {
        return g1MaxNewSizePercent;
    }

    public String getG1MixedGCCountTarget() {
        return g1MixedGCCountTarget;
    }

    public String getG1MixedGCLiveThresholdPercent() {
        return g1MixedGCLiveThresholdPercent;
    }

    public String getG1NewSizePercent() {
        return g1NewSizePercent;
    }

    public String getG1ReservePercent() {
        return g1ReservePercent;
    }

    public String getG1SummarizeRSetStats() {
        return g1SummarizeRSetStats;
    }

    public String getG1SummarizeRSetStatsPeriod() {
        return g1SummarizeRSetStatsPeriod;
    }

    /**
     * @return The garbage collector(s) based on the JVM options.
     */
    public List<GarbageCollector> getGarbageCollectors() {
        List<GarbageCollector> collectors = new ArrayList<GarbageCollector>();
        if (JdkUtil.isOptionEnabled(useSerialGc)) {
            collectors.add(GarbageCollector.SERIAL_NEW);
            collectors.add(GarbageCollector.SERIAL_OLD);
        }
        if (JdkUtil.isOptionEnabled(useParallelOldGc)) {
            collectors.add(GarbageCollector.PARALLEL_SCAVENGE);
            collectors.add(GarbageCollector.PARALLEL_OLD);
        } else if (JdkUtil.isOptionEnabled(useParallelGc)) {
            collectors.add(GarbageCollector.PARALLEL_SCAVENGE);
            if (JdkUtil.isOptionDisabled(useParallelOldGc)) {
                collectors.add(GarbageCollector.PARALLEL_SERIAL_OLD);
            } else {
                collectors.add(GarbageCollector.PARALLEL_OLD);
            }
        }
        if (JdkUtil.isOptionEnabled(useConcMarkSweepGc)) {
            if (useParNewGc == null || JdkUtil.isOptionEnabled(useParNewGc)) {
                collectors.add(GarbageCollector.PAR_NEW);
            } else {
                collectors.add(GarbageCollector.SERIAL_NEW);
            }
            collectors.add(GarbageCollector.CMS);
            if (!JdkUtil.isOptionEnabled(useCmsCompactAtFullCollection)) {
                collectors.add(GarbageCollector.SERIAL_OLD);
            }
        } else if (JdkUtil.isOptionEnabled(useParNewGc)) {
            collectors.add(GarbageCollector.PAR_NEW);
            collectors.add(GarbageCollector.SERIAL_OLD);
        }
        if (JdkUtil.isOptionEnabled(useG1Gc)) {
            collectors.add(GarbageCollector.G1);
        }
        if (JdkUtil.isOptionEnabled(useShenandoahGc)) {
            collectors.add(GarbageCollector.SHENANDOAH);
        }
        if (JdkUtil.isOptionEnabled(useZGc)) {
            collectors.add(GarbageCollector.ZGC);
        }
        if (collectors.size() == 0) {
            collectors = JdkUtil.getDefaultGarbageCollectors(jvmContext.getVersionMajor());
        }
        return collectors;
    }

    public String getGcLockerRetryAllocationCount() {
        return gcLockerRetryAllocationCount;
    }

    public String getGcLogFileSize() {
        return gcLogFileSize;
    }

    public String getGcTimeRatio() {
        return gcTimeRatio;
    }

    public String getGuaranteedSafepointInterval() {
        return guaranteedSafepointInterval;
    }

    public String getHeapBaseMinAddress() {
        return heapBaseMinAddress;
    }

    public String getHeapDumpOnOutOfMemoryError() {
        return heapDumpOnOutOfMemoryError;
    }

    public String getHeapDumpPath() {
        return heapDumpPath;
    }

    public String getIgnoreUnrecognizedVmOptions() {
        return ignoreUnrecognizedVmOptions;
    }

    public String getInitialBootClassLoaderMetaspaceSize() {
        return initialBootClassLoaderMetaspaceSize;
    }

    public String getInitialHeapSize() {
        return initialHeapSize;
    }

    public String getInitiatingHeapOccupancyPercent() {
        return initiatingHeapOccupancyPercent;
    }

    public ArrayList<String> getJavaagent() {
        return javaagent;
    }

    public JvmContext getJvmContext() {
        return jvmContext;
    }

    public String getLargePageSizeInBytes() {
        return largePageSizeInBytes;
    }

    public ArrayList<String> getLog() {
        return log;
    }

    public String getLogFile() {
        return logFile;
    }

    public String getLoggc() {
        return loggc;
    }

    public String getLogVmOutput() {
        return logVmOutput;
    }

    public String getLoopStripMiningIter() {
        return loopStripMiningIter;
    }

    public String getManagementServer() {
        return managementServer;
    }

    public String getMarkStackSize() {
        return markStackSize;
    }

    public String getMarkStackSizeMax() {
        return markStackSizeMax;
    }

    public String getMaxDirectMemorySize() {
        return maxDirectMemorySize;
    }

    public String getMaxFdLimit() {
        return maxFdLimit;
    }

    public String getMaxGcPauseMillis() {
        return maxGcPauseMillis;
    }

    public String getMaxHeapFreeRatio() {
        return maxHeapFreeRatio;
    }

    public String getMaxHeapSize() {
        return maxHeapSize;
    }

    public String getMaxInlineLevel() {
        return maxInlineLevel;
    }

    public String getMaxJavaStackTraceDepth() {
        return maxJavaStackTraceDepth;
    }

    public String getMaxjitcodesize() {
        return maxjitcodesize;
    }

    public String getMaxMetaspaceSize() {
        return maxMetaspaceSize;
    }

    public String getMaxNewSize() {
        return maxNewSize;
    }

    public String getMaxPermSize() {
        return maxPermSize;
    }

    public String getMaxTenuringThreshold() {
        return maxTenuringThreshold;
    }

    public String getMetaspaceSize() {
        return metaspaceSize;
    }

    public String getMinHeapDeltaBytes() {
        return minHeapDeltaBytes;
    }

    public String getMinHeapFreeRatio() {
        return minHeapFreeRatio;
    }

    public String getNativeMemoryTracking() {
        return nativeMemoryTracking;
    }

    public String getNewRatio() {
        return newRatio;
    }

    public String getNewSize() {
        return newSize;
    }

    public String getNumberOfGcLogFiles() {
        return numberOfGcLogFiles;
    }

    public String getOldPlabSize() {
        return oldPlabSize;
    }

    public String getOldSize() {
        return oldSize;
    }

    public String getOmitStackTraceInFastThrow() {
        return omitStackTraceInFastThrow;
    }

    public String getOnError() {
        return onError;
    }

    public String getOnOutOfMemoryError() {
        return onOutOfMemoryError;
    }

    public String getOptimizeStringConcat() {
        return optimizeStringConcat;
    }

    public Map<String, ArrayList<String>> getOptions() {
        return options;
    }

    public ArrayList<String> getOverconstrained() {
        return overconstrained;
    }

    public String getParallelGcThreads() {
        return parallelGcThreads;
    }

    public String getParallelRefProcEnabled() {
        return parallelRefProcEnabled;
    }

    public String getPerfDisableSharedMem() {
        return perfDisableSharedMem;
    }

    public String getPerMethodRecompilationCutoff() {
        return perMethodRecompilationCutoff;
    }

    public String getPermSize() {
        return permSize;
    }

    public String getPrintAdaptiveSizePolicy() {
        return printAdaptiveSizePolicy;
    }

    public String getPrintClassHistogram() {
        return printClassHistogram;
    }

    public String getPrintClassHistogramAfterFullGc() {
        return printClassHistogramAfterFullGc;
    }

    public String getPrintClassHistogramBeforeFullGc() {
        return printClassHistogramBeforeFullGc;
    }

    public String getPrintCodeCache() {
        return printCodeCache;
    }

    public String getPrintCommandLineFlags() {
        return printCommandLineFlags;
    }

    public String getPrintFlagsFinal() {
        return printFlagsFinal;
    }

    public String getPrintFLSStatistics() {
        return printFLSStatistics;
    }

    public String getPrintGc() {
        return printGc;
    }

    public String getPrintGcApplicationConcurrentTime() {
        return printGcApplicationConcurrentTime;
    }

    public String getPrintGcApplicationStoppedTime() {
        return printGcApplicationStoppedTime;
    }

    public String getPrintGcCause() {
        return printGcCause;
    }

    public String getPrintGcDateStamps() {
        return printGcDateStamps;
    }

    public String getPrintGcDetails() {
        return printGcDetails;
    }

    public String getPrintGcTaskTimeStamps() {
        return printGcTaskTimeStamps;
    }

    public String getPrintGcTimeStamps() {
        return printGcTimeStamps;
    }

    public String getPrintHeapAtGc() {
        return printHeapAtGc;
    }

    public String getPrintHeapAtGC() {
        return printHeapAtGc;
    }

    public String getPrintPromotionFailure() {
        return printPromotionFailure;
    }

    public String getPrintReferenceGc() {
        return printReferenceGc;
    }

    public String getPrintSafepointStatistics() {
        return printSafepointStatistics;
    }

    public String getPrintStringDeduplicationStatistics() {
        return printStringDeduplicationStatistics;
    }

    public String getPrintStringTableStatistics() {
        return printStringTableStatistics;
    }

    public String getPrintTenuringDistribution() {
        return printTenuringDistribution;
    }

    public String getReservedCodeCacheSize() {
        return reservedCodeCacheSize;
    }

    public String getResizePlab() {
        return resizePlab;
    }

    public String getResizeTlab() {
        return resizeTlab;
    }

    public ArrayList<String> getRunjdwp() {
        return runjdwp;
    }

    public String getShenandoahGcHeuristics() {
        return shenandoahGcHeuristics;
    }

    public String getShenandoahGuaranteedGCInterval() {
        return shenandoahGuaranteedGCInterval;
    }

    public String getShenandoahMinFreeThreshold() {
        return shenandoahMinFreeThreshold;
    }

    public String getShenandoahUncommitDelay() {
        return shenandoahUncommitDelay;
    }

    public String getSoftRefLRUPolicyMSPerMB() {
        return softRefLRUPolicyMSPerMB;
    }

    public String getStringTableSize() {
        return stringTableSize;
    }

    /**
     * Client Distributed Garbage Collection (DGC) interval in milliseconds.
     * 
     * <pre>
     * -Dsun.rmi.dgc.client.gcInterval=14400000
     * </pre>
     * 
     * @return The client Distributed Garbage Collection (DGC), or null if not explicitly set.
     */
    public String getSunRmiDgcClientGcInterval() {
        String sunRmiDgcClientGcIntervalOption = null;
        if (!systemProperties.isEmpty()) {
            Iterator<String> iterator = systemProperties.iterator();
            while (iterator.hasNext()) {
                String property = iterator.next();
                if (property.matches("-Dsun.rmi.dgc.client.gcInterval=\\d{1,}")) {
                    sunRmiDgcClientGcIntervalOption = property;
                    break;
                }
            }
        }
        return sunRmiDgcClientGcIntervalOption;
    }

    /**
     * Server Distributed Garbage Collection (DGC) interval in milliseconds.
     * 
     * <pre>
     * -Dsun.rmi.dgc.server.gcInterval=14400000
     * </pre>
     * 
     * @return The server Distributed Garbage Collection (DGC), or null if not explicitly set.
     */
    public String getSunRmiDgcServerGcInterval() {
        String sunRmiDgcServerGcIntervalOption = null;
        if (!systemProperties.isEmpty()) {
            Iterator<String> iterator = systemProperties.iterator();
            while (iterator.hasNext()) {
                String property = iterator.next();
                if (property.matches("-Dsun.rmi.dgc.server.gcInterval=\\d{1,}")) {
                    sunRmiDgcServerGcIntervalOption = property;
                    break;
                }
            }
        }
        return sunRmiDgcServerGcIntervalOption;
    }

    public String getSurvivorRatio() {
        return survivorRatio;
    }

    public ArrayList<String> getSystemProperties() {
        return systemProperties;
    }

    public String getTargetSurvivorRatio() {
        return targetSurvivorRatio;
    }

    public String getThreadPriorityPolicy() {
        return threadPriorityPolicy;
    }

    public String getThreadStackSize() {
        return threadStackSize;
    }

    public String getTier2CompileThreshold() {
        return tier2CompileThreshold;
    }

    public String getTier3CompileThreshold() {
        return tier3CompileThreshold;
    }

    public String getTier4CompileThreshold() {
        return tier4CompileThreshold;
    }

    public String getTieredCompilation() {
        return tieredCompilation;
    }

    public String getTraceClassLoading() {
        return traceClassLoading;
    }

    public String getTraceClassUnloading() {
        return traceClassUnloading;
    }

    /**
     * JVM options that are disabled that are not accounted for in the current analysis Is there is a valid use case for
     * the option being disabled (and it should be added to the list of accounted for options)? Or should an
     * option-specific analysis be added?
     * 
     * @return the unaccounted disabled JVM options, null otherwise.
     */
    public String getUnaccountedDisabledOptions() {
        String accountedDisabledOptions = "-XX:-BackgroundCompilation -XX:-ClassUnloading "
                + "-XX:-CMSClassUnloadingEnabled -XX:-CMSParallelInitialMarkEnabled -XX:-CMSParallelRemarkEnabled "
                + "-XX:-ExplicitGCInvokesConcurrentAndUnloadsClasses -XX:-HeapDumpOnOutOfMemoryError "
                + "-XX:-PrintAdaptiveSizePolicy -XX:-PrintGCCause -XX:-PrintGCDateStamps -XX:-PrintGCDetails "
                + "-XX:-PrintGCTimeStamps -XX:-TraceClassUnloading -XX:-UseAdaptiveSizePolicy -XX:-UseBiasedLocking "
                + "-XX:-UseCompressedClassPointers -XX:-UseCompressedOops -XX:-UseGCLogFileRotation "
                + "-XX:-UseGCOverheadLimit -XX:-UseLargePagesIndividualAllocation -XX:-UseParallelOldGC "
                + "-XX:-UseParNewGC -XX:-TieredCompilation";

        String unaccountedDisabledOptions = null;
        for (String disabledOption : getDisabledOptions()) {
            if (accountedDisabledOptions.lastIndexOf(disabledOption) == -1
                    && !getUndefined().contains(disabledOption)) {
                unaccountedDisabledOptions = unaccountedDisabledOptions == null ? disabledOption
                        : unaccountedDisabledOptions + ", " + disabledOption;
            }
        }
        return unaccountedDisabledOptions;
    }

    public ArrayList<String> getUndefined() {
        return undefined;
    }

    public String getUnlockDiagnosticVmOptions() {
        return unlockDiagnosticVmOptions;
    }

    public String getUnlockExperimentalVmOptions() {
        return unlockExperimentalVmOptions;
    }

    public String getUnsyncloadClass() {
        return unsyncloadClass;
    }

    public String getUseAdaptiveSizePolicy() {
        return useAdaptiveSizePolicy;
    }

    public String getUseAvx() {
        return useAvx;
    }

    public String getUseBiasedLocking() {
        return useBiasedLocking;
    }

    public String getUseCGroupMemoryLimitForHeap() {
        return useCGroupMemoryLimitForHeap;
    }

    public String getUseCmsCompactAtFullCollection() {
        return useCmsCompactAtFullCollection;
    }

    public String getUseCmsInitiatingOccupancyOnly() {
        return useCmsInitiatingOccupancyOnly;
    }

    public String getUseCodeCacheFlushing() {
        return useCodeCacheFlushing;
    }

    public String getUseCompressedClassPointers() {
        return useCompressedClassPointers;
    }

    public String getUseCompressedOops() {
        return useCompressedOops;
    }

    public String getUseConcMarkSweepGc() {
        return useConcMarkSweepGc;
    }

    public String getUseCondCardMark() {
        return useCondCardMark;
    }

    public String getUseContainerSupport() {
        return useContainerSupport;
    }

    public String getUseCountedLoopSafepoints() {
        return useCountedLoopSafepoints;
    }

    public String getUseDynamicNumberOfCompilerThreads() {
        return useDynamicNumberOfCompilerThreads;
    }

    public String getUseDynamicNumberOfGcThreads() {
        return useDynamicNumberOfGcThreads;
    }

    public String getUseFastAccessorMethods() {
        return useFastAccessorMethods;
    }

    public String getUseFastUnorderedTimeStamps() {
        return useFastUnorderedTimeStamps;
    }

    public String getUseG1Gc() {
        return useG1Gc;
    }

    public String getUseGcLogFileRotation() {
        return useGcLogFileRotation;
    }

    public String getUseGcOverheadLimit() {
        return useGcOverheadLimit;
    }

    public String getUseHugeTLBFS() {
        return useHugeTLBFS;
    }

    public String getUseLargePages() {
        return useLargePages;
    }

    public String getUseLargePagesIndividualAllocation() {
        return useLargePagesIndividualAllocation;
    }

    public String getUseMembar() {
        return useMembar;
    }

    public String getUseNUMA() {
        return useNUMA;
    }

    public String getUseParallelGc() {
        return useParallelGc;
    }

    public String getUseParallelOldGc() {
        return useParallelOldGc;
    }

    public String getUseParNewGc() {
        return useParNewGc;
    }

    public String getUsePerfData() {
        return usePerfData;
    }

    public String getUseSerialGc() {
        return useSerialGc;
    }

    public String getUseShenandoahGc() {
        return useShenandoahGc;
    }

    public String getUseSplitVerifier() {
        return useSplitVerifier;
    }

    public String getUseStringDeduplication() {
        return useStringDeduplication;
    }

    public String getUseThreadPriorities() {
        return useThreadPriorities;
    }

    public String getUseTlab() {
        return useTlab;
    }

    public String getUseVmInterruptibleIo() {
        return useVmInterruptibleIo;
    }

    public String getVerify() {
        return verify;
    }

    /**
     * Convenience method to check <code>Analysis</code> existence.
     * 
     * @param key
     *            The <code>Analysis</code> to check.
     * @return True if the <code>Analysis</code> exists, false otherwise.
     */
    public boolean hasAnalysis(Analysis key) {
        return analysis.contains(key);
    }

    /**
     * @param key
     *            The <code>Analysis</code> key to check.
     * @return True if the <code>Analysis</code> exists, false otherwise.
     */
    public boolean hasAnalysis(String key) {
        boolean hasAnalysis = false;
        Iterator<Analysis> iterator = analysis.iterator();
        while (iterator.hasNext()) {
            Analysis entry = iterator.next();
            if (entry.getKey().equals(key)) {
                hasAnalysis = true;
                break;
            }
        }
        return hasAnalysis;
    }

    public boolean isBatch() {
        return batch;
    }

    public boolean isCheckJni() {
        return checkJni;
    }

    public boolean isClient() {
        return client;
    }

    public boolean isComp() {
        return comp;
    }

    /**
     * @return True if the JVM is using compressed class pointers, false otherwise.
     */
    public boolean isCompressedClassPointers() {
        boolean isCompressedClassPointers = true;
        // Cannot use compressed pointers if compressed oops disabled
        if (!isCompressedOops() || JdkUtil.isOptionDisabled(getUseCompressedClassPointers())) {
            isCompressedClassPointers = false;
        }
        return isCompressedClassPointers;
    }

    /**
     * @return True if the JVM is using compressed object pointers, false otherwise.
     */
    public boolean isCompressedOops() {
        boolean isCompressedOops = true;
        if (JdkUtil.isOptionDisabled(useCompressedOops)) {
            // Disabled
            isCompressedOops = false;
        } else {
            // Default behavior based on heap size
            long thirtyTwoGigabytes = JdkUtil.convertSize(32, 'G', 'B');
            long heapMaxSize = JdkUtil.getByteOptionBytes(maxHeapSize);
            if (heapMaxSize >= thirtyTwoGigabytes) {
                isCompressedOops = false;
            }
        }
        return isCompressedOops;
    }

    public boolean isConcurrentio() {
        return concurrentio;
    }

    public boolean isD64() {
        return d64;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * @return true if JVM options result in using the default garbage collector, false otherwise.
     */
    private final boolean isDefaultCollector() {
        boolean useDefaultCollector = false;
        if (useSerialGc == null && useParNewGc == null && useConcMarkSweepGc == null && useParallelGc == null
                && useG1Gc == null && useShenandoahGc == null && useZGc == null) {
            useDefaultCollector = true;
        }
        return useDefaultCollector;
    }

    /**
     * @return True if the GC logging is enabled, false otherwise.
     */
    private boolean isGcLoggingEnable() {
        boolean isGcLoggingEnabled = false;
        if (loggc != null || !log.isEmpty() || printGc != null || printGcDetails != null || printGcTimeStamps != null
                || printGcDateStamps != null || printGcApplicationStoppedTime != null) {
            isGcLoggingEnabled = true;
        }
        return isGcLoggingEnabled;
    }

    /**
     * @return True if the GC logging is sent to stdout, false otherwise.
     */
    public boolean isGcLoggingToStdout() {
        boolean isGcLoggingStdout = false;
        if (isGcLoggingEnable()) {
            if (loggc == null && log.isEmpty()) {
                // JDK8
                isGcLoggingStdout = true;
            } else {
                // JDK9+
                Iterator<String> i = log.iterator();
                while (i.hasNext()) {
                    String gcLoggingOption = i.next();
                    isGcLoggingStdout = true;
                    if (gcLoggingOption.matches("^.+file.+$")) {
                        isGcLoggingStdout = false;
                        break;
                    } else {
                        isGcLoggingStdout = true;
                    }
                }
            }
        }
        return isGcLoggingStdout;
    }

    public boolean isNoclassgc() {
        return noclassgc;
    }

    public boolean isNoverify() {
        return noverify;
    }

    public boolean isRs() {
        return rs;
    }

    public boolean isServer() {
        return server;
    }

    public boolean isVerboseClass() {
        return verboseClass;
    }

    public boolean isVerboseGc() {
        return verboseGc;
    }

    public boolean isxInt() {
        return xInt;
    }

    /**
     * Convenience method to remove <code>Analysis</code>.
     * 
     * @param key
     *            The <code>Analysis</code> to check.
     */
    public void removeAnalysis(Analysis key) {
        analysis.remove(key);
    }
}
