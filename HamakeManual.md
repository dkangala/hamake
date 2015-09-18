version 2.0 beta



# Introduction #

Hamake is a lightweight utility and workflow engine for Hadoop. Hamake helps to organize your Hadoop Map Reduce jobs, Pig script and local programs in a workflow and launch them based on dataflow principles - your tasks will be executed as soon as new data will be availible for them.

## Why Hamake? ##

Hadoop jobs are not run alone by themselfs. Usually you require multiple Hadoop jobs to achieve some result. Also sometines you would like to filter your data first and process only files that have changed. Hamake helps you to organize your Hadoop map-reduce jobs or Pig scripts in a workflow, that will be launched and controlled from a single machine. The order of execution of Hadoop map-reduce jobs or Pig scripts in hamake is based on dataflow programming model and is controlled by data, not by tasks themselfs. Hamake is very simple in installation and configuration. In fact all you need to install Hamake is to copy a single jar file to the directory of your choise.

# Installation #

Hamake runs in Hadoop environment. Please make sure you have installed Hadoop on the machine where you are going to run Hamake and added Hadoop bin directory in PATH environment variable.

```
export PATH=$PATH:$HADOOP_HOME/bin
```
In contrast to similar software Hamake does not require dedicated server or complex installation. To install Hamake just unpack Hamake distribution archive and copy hamake-2.0b.jar file from the folder where you have unpacked distribution archive to the directory of your choise. Next you should describe workflow definitions in a special-formatted hamake-file and launch the process with _hadoop_ command:
> Example: `hadoop jar hamake-2.0b.jar -f path_to_hamake-file`

# Using Hamake #

Hamake is a workflow engine. To understand how it works here are simple steps:
  1. You define your tasks in a special XML-formatted file (hamake-file) and launch Hamake as Hadoop job giving hamake-file as an argument.
  1. Hamake arranges tasks from hamake-file in a direct acyclic graph
  1. Tasks are launched as soon as new data is availible for them
  1. In case all wend down well, you recieve your output
  1. In case some tasks failed, Hamake output error message and tries to finish as many tasks as it can

To get familiar with hamake-file syntax please visit [Syntax Reference](http://code.google.com/p/hamake/wiki/newSyntaxReference) page.

## Direct Acyclic Graph ##

Frankly speaking, Hamake does not strictly follows [Dataflow](http://en.wikipedia.org/wiki/Dataflow) programming paradigm. Hamake runs tasks based on _Direct Acyclic Graph_ (DAG) that it builds at the beginning. Graph is build based on task input and output where graph edge is data and graph node is a task. If output of task A intercepts with input of task B the task B is considered to be dependent on task A. Tasks are then executed in order from root nodes to leaf nodes. Task is considered to be ready for execution as soon as its parents have finished processing data.

Hamake builds and runs DAG in a following way:
  1. At the beginning Hamake loads all tasks from hamake-file into memory and searches for tasks that has no input or input data that does not intercept with other tasks output. This tasks becomes _root tasks_.
  1. Hamake selects root tasks to start with and launches them in parallel
  1. As soon as some task has finished, it is marked as completed and the child task is launched
  1. When no tasks are left, Hamake exits

Graph is stored in the memory of the machine where Hamake is running, so the number of tasks are limited by the upper bound of the JVM heap.

You can control how many root tasks are selected by two ways:
  * You can list leaf tasks you would like to end with as Hamake command-line parameters
> > Example: `hadoop jar hamake-2.0b.jar jar-listings filter-listings`
  * You can specify default leaf task you would like to end with in hamake-file. This attribute is taken into account only in case you have not specified leaf tasks to start with as Hamake command-line parameters.
> > Example: `<project name="test" default="jar-listings">`

## Hamake Dependency Mechanism ##

In Hamake first task depends on second if input data of first task is an output data of a second. If first task depends on second it will be launched as soon as second task has finished. Tasks may be skipped if they have been executed earlier and thier output is fresh enough. In other words, Hamake runs tasks incrementally, task won't be executed if input data are older than output data.

Lets imagine you have three map-reduce jobs you would like to combine in a workflow. First job collects log files from your servers on HDFS. Second job extracts top N of most active users and puts this list in a distributed cache. Third job counts recommendations for a user based on extracted user list and collected log files. In dataflow programming model you can put third job dependent on the second job by specifying that output folder of the second job will be an input of the third job. But how can you put third job dependent on the first one?

In Hamake you can also define some task to be dependent on external resources. In case of such dependency task will be executed only if external data is present and fresh. Also please note that external resources will not get into task input. Thus, you can define third task to be dependent on the first one by specifying its output folder as external resource.

Each task is executed in a separate thread, therefore tasks which does not depends on each other will be executed in parallel.

In case some task has failed, Hamake will not execute subsequent tasks from the same DAG branch. For example, if graph there are two independent branches in a DAG with the root task A:

A -> B -> C 

&lt;BR&gt;


A -> E -> D

In case failure has occurred during execution of task B Hamake will not execute task C but  will execute D and E tasks

## Path Generation ##

Since Hamake operates on direct acyclic graph there is a mechanism that helps to prevent cyclic dependencies in your hamak-file. This mechanism is called _generation_.   _generation_ is a natural number that explicitly indicates that one path differs from another. To indicate that output path is not the same as input path you should set output _generation_ to be greater then input _generation_. Every path in Hamake has its _generation_ number. By default it is 0. If you have some DTR that outputs data to the same location where other DTR gets its data, you should specify _generation_ attribute of the output data location tag or the the branch will be silently ignored.

## Data Mapping ##

When designing flow of your process with Hamake you have an option between two data mapping types:

  * You can map each file from input folder to file from one or many output folder(s) with `<foreach>` tag
  * You can map a set of file(s) or folder(s) from input to a set of output file(s) of folder(s) `<fold>` tag

In case you map your files with `<foreach>` tag, for each file Hamake will produce one file in one or many corresponding output folders. Hamake will launch task for every file from inpur folder. If we view _foreach_ as a function, we can define it using Haskell language syntax as:
```
foreach:: Path -> [Path] -> [Path]
map source dependencies targets = ...
```
`<fold>` simply maps a collection of data from input to a collection of data from output. Hamake launches simgle task to process input data and output result. If we view _fold_ as a function, we can define it using Haskell language syntax as:
```
fold:: [Path] -> [Path]
fold source targets = ...
```

## Hamake Tasks ##

Hamake operates with three types of tasks which are implemented by user. Threse are Hadoop MR jobs, Pig scripts and arbitrary program or bash script that is launched on a machine where Hamake is running. You define these tasks in hamake-file.

Example:
TODO: add example of three types of tasks


## Tasks Submission ##

Currently you can define following types of tasks:
| **Task** | **What Hamake Does** |
|:---------|:---------------------|
| Hadoop map-reduce job | Hamake copies MR job jar to local temporary location and then submits job to JobTracker. |
| pig      | Hamake copies PigLatin script to local temporary location and then executes this script.|
| exec     | Executes specified script or program locally on the machine, where Hamake is running |

## Working directory ##
Hamake resolves all non-absolute paths defined in hamake-file relative to working directory. Working directory can be defined as the Hamake command-line argument with _-w_ option.

> Example: `hadoop jar hamake-2.0b.jar -w /home/hadoop/scripts`

If working directory has been omitted, it will be determined by Hamake as:
  * user home directory, if Hadoop default FS is local FS (`file://`)
  * /user/<user name>, if Hadoop default FS is HDFS (hdfs://)

## Paths ##
Hamake takes into account schema part of the paths used in workflow script. Thus, for example, if Hamake works on Amazon Elastic MapReduce, it is possible to specify paths to data stored on S3:
```
<property name="output"  value="s3://hamake/test/"/>
```

# Running Hamake #
If you installed Hamake as described in _Installation_ you can run Hamake from the command-line in a following way:
```
hadoop jar path_to_hamake.jar/hamake-2.0b.jar path_to_hamake-file
```

To run Hamake you should have Hadoop installed and bin folder of Hadoop should be added to system PATH environment variable.

## Command-line options summary ##

hadoop jar path\_to\_hamake.jar/hamake-2.0b.jar path\_to\_hamake-file `[options] [target [target2] [target3] ...]`
Options:
> -V, -version display version and exit.

&lt;BR&gt;


> -v, -verbose verbose mode.

&lt;BR&gt;


> -n, -dry-run execution simulation without actually executing any tasks or modifying any files.

&lt;BR&gt;


> -j, -jobs execute up to N tasks simultaneously. Applies only to independent tasks. By default N is unlimited. -j 1 will make it execute tasks one by one.

&lt;BR&gt;


> -f, -file specifies makefile name. If this option is omitted 'hamakefile.xml' is assumed

&lt;BR&gt;


> -w, -workdir path to working directory