# Adaptive Behavioral Model Learning for Software Product Lines (Artifact Submission)

## Introduction
In this submission, the artifacts of the paper ``Adaptive Behavioral Model Learning for Software Product Lines'' (Submission number 16) are described. The artifacts include models of the subject systems, source code of the experiments, and statistical tests used. In this paper, the structure of the benchmarks is described; the functionality of the different parts of the source code is explained; and the steps of the performed experiments are elaborated. We also specify how the statistical tests are performed.

## Subject Systems
The two subject systems used in the experiments are the Minepump SPL and the BCS SPL. For each of these SPLs, there is a folder of the same name in the **experiments** folder. The contents of these folders are described below:

### 1) The Minepump SPL:
The artifacts of the Minepump SPL are available in the **Minepump_SPL** directory. This folder contains a file named **model.xml** which is the feature model \cite{kang1990feature,DBLP:conf/re/SchobbensHT06} of this SPL. Feature model files are available in ``.xml'' format \cite{DBLP:journals/scp/ThumKBMSL14} and are created using the FeatureIDE \cite{DBLP:journals/scp/ThumKBMSL14} library.
Model learning experiments are performed using finite state machines (FSM) \cite{gill1962introduction}.
The FSM files and the configuration files for the products in the sample are in folder \texttt{products\_3wise}.
The FSM files are saved in ``.dot'' format \cite{Gansner00anopen,Ellson01graphviz—} and the configuration files are in ``.config'' format \cite{DBLP:journals/scp/ThumKBMSL14}.
### 2) The BCS SPL:
The artifacts of the BCS SPL are stored in the * *BCS_SPL* * folder. The **model.xml** file is the feature model of this SPL. The FSM files for the BCS components are available in the \texttt{Complete\_FSM\_files} folder. The component FSMs are created using the I/O transition systems which are available in \cite{lity2013delta}. The FSM of each valid product, can be created by merging the FSMs of its components.

## The Source Code Artifacts

In the paper, the model learning experiments are performed using the \texttt{ExtensibleLStarMealyBuilder} class of the LearnLib \cite{DBLP:conf/fmics/RaffeltSB05} library version 0.16.0.
The source code artifacts of this paper are in the \texttt{ir.ac.ut.fml} package from the \texttt{src} directory. The function of classes in this package is described below:

The \texttt{MergeMultipleFSMs} class is used to produce the FSMs of the BCS SPL products. The input parameters of this class are listed below:
\begin{itemize}
    \item -dir: Directory of the configuration files
    \item -dir2: Folder containing FSMs of SPL components
    \item -out: Output directory for storing FSMs of SPL products.
    \item -fm: Feature model
\end{itemize}
Using the \texttt{MergeMultipleFSMs} class, for each of the product configuration files, the FSM files of its features are merged and the FSM of that product is constructed.
The configuration files of the BCS SPL are available in the \texttt{products\_3wise} folder of the \texttt{BCS\_SPL} directory.
The FSM files for the components of the BCS SPL are available in the \texttt{Complete\_FSM\_files}..

The \texttt{LearningOrderSampling} class, as input, takes a folder containing the products sampled from an SPL (i.e., the subject system SPLs). The products in this sample are learned based on different random orders using the $\text{PL}^*$ method and the non-adaptive method. For each learning order, the values of the learning cost metrics for each learning method are measured and stored in a log file.
By running the \texttt{FixedLearningOrder} class, the products in a sample are learned based on a fixed learning order using the $\text{PL}^*$ method. This process is repeated several times and the values of the learning cost metrics are stored in a log file.
The \texttt{Calculate order metric} class is used to calculate the values of parameter $D$ for a number of random learning orders. After running this class, the calculated values of parameter $D$ are stored in a log file.

The three classes \texttt{ConvertToExcelFile}, \texttt{ConvertToExcelFile2} and \texttt{ConvertToExcelFile3} are used in Experiments \ref{experiment_1}, \ref{experiment_2_1} and \ref{experiment_2_2}, respectively, to convert the created log file to a ``.csv'' file.


\section{Replicating the Experiments}\label{experiments}
To replicate the experiments, the repository must be downloaded. All the files needed for these experiments are in the \texttt{experiments} folder. The steps of replicating the experiments is described below:

\subsection{Comparing the learning methods (RQ1-RQ3)}\label{experiment_1}
To replicate this experiment, the \texttt{LearningOrderSampling} class must be run using the following parameters:

\begin{itemize}
    \item -dir: Directory of the SPL products (sampled products)
    \item -out: The output directory (the log file will be saved in this directory)
    \item -sot2: A folder for storing observation tables and learned FSM files
\end{itemize}
It is not necessary to specify the values of other arguments because they have default values.
If the ``-help'' argument is used, the help menu is displayed.
The number of learning orders tested is determined using the \texttt{samples\_count} variable, which in this experiment is set to 200. 

The learning orders used and their corresponding metric values, which are stored in the log file, must be saved as a ``.csv'' file.
Each row of this ``.csv'' file contains a learning order and the corresponding learning cost metric values (for the $\text{PL}^*$ method and the non-adaptive method)
To do this, the \texttt{ConvertToExcelFile} class must be run with the following parameters:

\begin{itemize}
    \item -file: The input log file
    \item -out: The output directory (the ``.csv'' file will be saved in this directory)
\end{itemize}

The ``.csv'' files of this experiment are in the \texttt{results\_1} folder.

\subsection{The effect of learning order (RQ4)}

To evaluate the effect of product learning order on the efficiency of the $\text{PL}^*$ method, two experiments are performed.
Experiment \ref{experiment_2_1} shows that the order of learning products can affect the total cost of learning in the $\text{PL}^*$ method. In Experiment \ref{experiment_2_2}, the value of parameter $D$ is calculated for the 200 learning orders used in Experiment \ref{experiment_1}. At the end of this experiment, the Pearson correlation coefficient between $D$ and the learning cost metrics can be calculated (using the Python codes explained in \ref{section:statistical_tests}).

\subsubsection{The effect of learning order on the efficiency of the $\text{PL}^*$ method}\label{experiment_2_1}
To replicate this experiment, a fixed learning order must first be considered. This learning order is stored in a variable of type int array (called \texttt{learning\_order\_array}) in the \texttt{FixedLearningOrder} class. The parameters required to run this class are similar to the parameters described for the \texttt{LearningOrderSampling} class. After running the \texttt{FixedLearningOrder} class, the measured values for the learning cost metrics are stored in a log file. Using the \texttt{ConverToExcelFile2} class, these values can be stored as a ``.csv'' file.
The above steps must be performed for two learning orders: an order with a high learning efficiency (order 1) and an order with a relatively low learning efficiency (order 2). To determine these two learning orders, the results of Experiment \ref{experiment_1}, which are sorted by efficiency, can be used. The sorted ``csv'' files of Experiment \ref{experiment_1} are in the \texttt{results\_1} directory.
The results of Experiment \ref{experiment_2_1} for both subject SPLs are available in the \texttt{results\_2\_1} directory.

\subsubsection{Calculating the parameter D}\label{experiment_2_2}
In this experiment, using the \texttt{CalculateOrderMetric} class, the values of parameter $D$ is calculated for the 200 learning orders used in Experiment \ref{experiment_1}.
The parameters required to run the \texttt{CalculateOrderMetric} class are similar to the parameters explained for the \texttt{LearningOrderSampling} class.
In order for random learning orders produced in this experiment and Experiment \ref{experiment_1} to be the same, the following conditions must be considered for the \texttt{CalculateOrderMetric} and \texttt{LearningOrderSampling} classes:

\begin{itemize}
    \item  The initial value of the seed must be the same in both classes. The seed value is stored in a \texttt{long} variable of the same name.
    \item The number of random learning orders generated in both classes must be the same. The number of product learning orders is stored in a variable called \texttt{samples\_count}.
\end{itemize}

After running the \texttt{CalculateOrderMetric} class, the values calculated for parameter $D$ are stored in a log file. Then, using the \texttt{ConvertToExcelFile3} class, theses values can be saved as a ``.csv'' file.
The results of Experiment \ref{experiment_2_2} are available as ``.csv'' files in the \texttt{results\_2\_2} folder.

\section{Statistical Tests}\label{section:statistical_tests}

The source codes for performing statistical tests and plotting diagrams are in the \texttt{SPL\_Learning.ipynb} file (which is located in the \texttt{statistical\_tests} folder). These codes are written in Python using the Jupyter Notebook.
To perform statistical tests, the ``.csv'' files generated in the experiments described in Section \ref{experiments} are first loaded using into DataFrames using the \texttt{read\_csv} method of the Pandas \cite{mckinney-proc-scipy-2010} library.
Statistil tests are performed using the Scipy \cite{SciPy} library.
The Matplotlib \cite{Hunter:2007} library is used to draw the diagrams.


\section{Licensing} 
The artifacts are all available under GNU Public License 3.0. 
It makes use of the following two repositories, which are also available under the same license and
are properly attributed in the artifact: 

https://github.com/damascenodiego/DynamicLstarM
\cite{DBLP:conf/ifm/DamascenoMS19}

https://github.com/damascenodiego/learningFFSM
\cite{DBLP:journals/ese/DamascenoMS21}
