The table below attempts to compare Hamake and similar workflow engines for Hadoop ([Oozie](http://github.com/tucu00/oozie1), [Azkaban](http://sna-projects.com/azkaban/), [Cascading](http://www.cascading.org/)) based on some key features. Although all of these systems could be used to solve similar problems, they differ significantly in design, philosophy, target user profile, usage scenarios, etc.  So our feature-wise comparison is in no way conclusive. Please use it as a guideline, but read respective systems documentation to understand better which one is more suitable for your problem.

| **Feature** | **Hamake** | **Oozie** | **Azkaban** | **Cascading** |
|:------------|:-----------|:----------|:------------|:--------------|
| workflow discription language | XML        | XML (xPDL based) | text file with key/value pairs | Java API      |
| dependencies mechanism | data-driven | explicit  | explicit    | explicit      |
| requires Servlet/JSP container | No         | Yes       | Yes         | No            |
| allows to track a workflow progress | console/log messages | web page  | web page    | Java API      |
| ability to schedule a Hadoop job execution at given time | no         | yes       | yes         | yes           |
| execution model | command line utility | daemon    | daemon      | API           |
| allows to run Pig Latin scripts | yes        | yes       | yes         | yes           |
| event notification | no         | no        | no          | yes           |
| requires installation | no         | yes       | yes         | no            |
| supported Hadoop version | 0.18+      | 0.20+     | currently unknown | 0.18+         |
| retries     | no         | at workflow node level | yes         | yes           |
| ability to run arbitrary commands | yes        | yes       | yes         | yes           |
| can be run on  Amazon EMR | yes        | no        | currently unknown | yes           |

From [FAQ](FAQ.md):

### What is the difference between Hamake and Cascading? ###

In short: [Cascading](http://www.cascading.org/) is an API, while 'hamake' is an utility. Some differences:
  * hamake does not require any custom programming. It helps to automate running your existing Hadoop tasks and PIG scripts
  * We found hamake especially suitable for incremental processing of datasets
  * You can use 'hamake' to automate tasks written in other languages, for example using _Hadoop streaming_

### How Hamake differs from Oozie and Azkaban? ###

Oozie and Azkaban are server-side systems that have to be installed and run as a service. Hamake is a lightweight client-side utility that does not require installation and has very simple syntax for workflow definition.  Most importantly, Hamake is built based on dataflow programming principles - your Hadoop tasks execution sequence is controlled by the data.