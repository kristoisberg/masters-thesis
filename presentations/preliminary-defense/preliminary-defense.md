---
marp: true
theme: default
paginate: true
header: Master's Thesis Defense
footer: Kristo Isberg | Tallinn University of Technology | 2026
author: Kristo Isberg
---

<style>
section::after {
  content: attr(data-marpit-pagination) ' / ' attr(data-marpit-pagination-total);
}
</style>

# Detection of SQL Antipatterns in jOOQ Database Access Code Using Large Language Models

&nbsp;
**Kristo Isberg**
Supervisor: Erki Eessaar, PhD
&nbsp;
Tallinn University of Technology
23.04.2026

<!--
Hi, I'm Kristo and my Master's thesis topic is Detection of SQL Antipatterns in jOOQ Database Access Code Using LLMs. My supervisor is Erki Eessaar.

I will first briefly go over the background to explain the relevance of my topic, and then my research goals, and how I fulfilled them along with results.
-->

---

# SQL Antipatterns

- "... a commonly occurring solution to a problem that generates decidedly negative consequences" (Brown et al., 1998)
- "... the most frequent missteps software developers naively make while using SQL" (Karwin, 2022)
- Can affect performance, maintainability, portability, and data integrity

<!--
SQL antipatterns are a class of issues in SQL database access code. They were first introduced by Bill Karwin in a similarly named book.

They describe common solutions to common problems in databases, for example, related to database schema design or querying data. These solutions may solve the original problem in the short term, but in the long term they will cause more harm than good, so those solutions turn into problems themselves. For example, by harming the performance of the application, the maintainability of it, the portability between different system architectures and database engines, and the integrity of data.
-->

---

# What is jOOQ?

- Java Object Oriented Querying
- Open-source Java library for interacting with SQL databases
- Highly popular, especially in industrial projects
- Integrates SQL into Java as a domain-specific language
- Leverages code generation heavily
- Provides strong type safety

<!--
jOOQ is an open-source library for performing database interactions in Java. Compared to object relational mappers or ORMs, it offers much more flexibility, since it doesn't abstract away SQL, but rather integrates SQL into Java as an embedded domain-specific language.

This makes it very popular in industrial projects, and it is often preferred for the most complex and critical use cases, where ORMs are not able to perform efficiently, or even at all.

It is also highly type safe, and to achieve that, it leverages code generation heavily.
-->

---

# SQL Antipatterns in Java: literature review

- Quantifiable performance impact
  - Fixing _Implicit Columns_ improved runtime and energy efficiency of local queries in mobile applications by more than 25%
- Prevalent in Java projects
  - _Implicit Columns_ affects every 50th query
  - Remain unfixed longer than traditional code smells
  - Authors suspect lack of awareness or low priority
- Two tools capable of statically detecting SQL Antipatterns in Java
  - None capable of detecting from jOOQ DSL

<!--
SQL antipatterns in Java have had some research around it. One study managed to quantify their impact on the performance and enerrgy consumption of mobile applications, and, for example, found that fixing the "Implicit Columns" antipattern improved runtimes and energy consumption by more than 25 percent. "Implicit Columns", in essence, means fetching too much information from a database by using the asterisk wildcard or some other blind projection.

Another study looked at their prevalence in Java projects. For example, the Implicit Columns antipattern impacts two queries out of every one hundred. Also, SQL antipatterns tend to remain unfixed for a longer time than traditional Java code smells, and often they never get fixed at all. The authors of that study speculate that the reason is that developers aren't aware of SQL antipatterns, or they don't see fixing them as high enough priority.

A couple of tools have been developed to statically detect SQL antipatterns in Java code. They work just fine, but they have one large limitation. They are designed to find plain SQL queries in the Java source code, and then perform the detection on those. However, most queries in projects using jOOQ are created using the jOOQ DSL, therefore such detectors are unable to find those queries.
-->

---

# Research Goals

- Development of an LLM-based static analysis tool for detecting SQL antipatterns
- Studying the prevalence and occurrence patterns of SQL antipatterns in jOOQ

<!--
The main goal of this thesis is filling this gap by developing an LLM-based static analysis tool for detecting SQL antipatterns. Another, secondary goal is to study the prevalence and occurrence patterns of existing projects using jOOQ to see how it compares to projects using plain SQL. This can be useful both to developers looking to avoid indulging in antipatterns, as well as the maintainers of jOOQ to make changes in their part, for example improve the documentation of dangerous APIs or deprecate them in favour of safer ones.
-->

---

# Research Questions

- **RQ1:** How do different LLMs compare in identifying SQL antipatterns in Java code using jOOQ for database access?
- **RQ2:** How do different prompting strategies affect LLMs' accuracy in identifying SQL antipatterns in Java code using jOOQ for database access?
- **RQ3:** How accurate is the developed LLM-based tool in detecting SQL antipatterns in Java code using jOOQ for database access?
- **RQ4:** What are the occurrence patterns of SQL antipatterns in Java code using jOOQ for database access?
- **RQ5:** Which jOOQ API methods are most frequently associated with SQL antipattern occurrences?

<!--
To help us in our goals, we formulated five research questions regarding LLMs' capability of detecting SQL antipatterns; the impact different prompting strategies have on it; the detection performance of our tool; and occurrence patterns of SQL antipatterns in existing software.
-->

---

# Dataset Creation

- Used GitHub API to mine for projects using jOOQ
  - Filtered out non-Java projects, duplicates, etc
  - Gathered **602** applicable projects
- The author annotated a subset of 10% projects
  - Stratified sampling using head-tail breaks to preserve size distribution
  - 4 large, 10 medium, 47 small projects
  - **1,562** total antipattern occurrences identified in these

<!--
As there weren't any datasets available on SQL antipatterns in jOOQ, we had to create one ourselves. First, we mined for software projects depending on jOOQ using the GitHub search API. Then we applied several filters to exclude projects, which were not suitable for our dataset, such as projects written languages that weren't Java, and duplicates. In the end, we were left with 602 projects.

We selected a subset of ten percent of projects to annotate as our ground truth. As the size distribution of projects was heavily skewed towards small projects, we wanted to preserve the original size distribution in that subset. We used head-tail breaks to divide projects into three size categories, and sampled ten percent of each.

And then we carefully annotated the subset.
-->

---

# Dataset Creation

- Internal consistency measured using Cohen's Kappa
  - Re-annotated 10% of files after one month
    $$ \kappa = \frac{p_o - p_e}{1 - p_e} = 0.834 $$
  - Result: _Almost perfect agreement_
- **61** projects split into training/validation/test sets
  - Split ratio of 34/33/33
  - Monte Carlo optimisation to achieve an optimal split

<!--
We employed a single-annotator setup, but verified the internal consistency of the annotations. After a wash-out period of one month, we re-annotated 10 percent of the files. We compared the two sets of annotations and calculated their Cohen's kappa, which showed that the annotations were in almost perfect agreement, and were suitable to be used as ground truth.

We then divided the annotations into training, validation, and test sets using Monte Carlo optimization to ensure a balanced distribution of antipatterns across all subsets.
-->

---

# Analysis of LLMs and prompting strategies

- Models:
  - OpenAI GPT-5.2 — reasoning model
  - Anthropic Claude 4.5 Opus — non-reasoning\* model
  - Z.ai GLM-5 — open model
  - OpenAI gpt-oss-120B — medium-sized open model
- Prompting strategies:
  - Zero-Shot — baseline
  - Few-Shot — providing examples
  - Chain-of-Thought — "Think step by step."
  - Tree-of-Thought — simulating a conversation between three experts

<!--
We included a set of four diverse models in our analysis. GPT-5.2, which was the state-of-the-art reasoning model at the time. GLM-5, which was the best-performing open source model. Claude Opus 4.5, which we used as a non-reasoning model by disabling its reasoning capabilities. And gpt-oss-120B, which was one of the top-performing medium-sized models at the time.

And we evaluated four different prompting strategies. Zero-shot, which we treated as the baseline. Few-shot, where we provided it with example input and output combinations. Chain-of-thought, where we asked the model to think step-by-step. And tree-of-thought, where the model was asked to simulate the interactions between 3 subject matter experts.
-->

---

# Analysis of LLMs and prompting strategies

- Designed prompts for each prompting strategy, evaluating against training set
- Evaluated performance for each LLM and strategy combination on validation set
  - Precision, Recall, F1-Score
- Localisation task: True Positive only if model found the correct lines according to Intersection over Union
  $$ \text{IoU} = \frac{|S_p \cap S_g|}{|S_p \cup S_g|} \ge 0.5 $$

<!--
We iteratively designed prompts for each prompting strategy, measuring their performance against the training set. Once we were happy with the prompts, we evaluated their performance with each LLM on the validation set, and calculated their F1-scores, while also keeping track of their costs, runtimes, and general stability.

Here lies one novelty in our study. So far, all studies examining LLMs' capabilities in detecting code smells have treated it as a classification task, just indicating which code smells the provided source file contains. In our study, the model also needs to locate the antipatterns, providing the exact line range where the antipattern occurrences reside. A prediction is only considered a true positive, if the prediction lines up by at least 50 percent with the ground truth.
-->

---

<style scoped>
h1 {
  font-size: 1.3em;
}

th, td {
  font-size: 0.8em;
}
</style>

# RQ1: How do different LLMs compare in identifying SQL antipatterns in Java code using jOOQ for database access?

- All large models demonstrated similar performance
  - Varying costs and runtimes
  - GPT-5.2 was more paranoid than others
- gpt-oss-120B suffered from anomalies degrading performance

| Model                               | Precision |  Recall  | F1-Score | Cost (USD) | Runtime (s) |
| :---------------------------------- | :-------: | :------: | :------: | :--------: | :---------: |
| GPT-5.2 (reasoning)                 |   0.85    | **0.92** | **0.88** |   26.16    |    2,423    |
| GLM-5 (reasoning)                   | **0.88**  |   0.89   | **0.88** |   11.11    |    5,775    |
| **Claude Opus 4.5 (non-reasoning)** | **0.88**  |   0.89   | **0.88** |   29.97    |   **367**   |
| gpt-oss-120B (reasoning)            |   0.83    |   0.87   |   0.83   |  **1.49**  |    2,808    |

<!--
To answer our first research question, we looked at the models' performance using the baseline zero-shot strategy.

The results were surprisingly close. All three of the large models demonstrated very similar performance regarding F1-score. The only difference was that GPT-5.2 was more paranoid than the others, meaning that it had fewer false negatives than the others, at an expense of more false positives.

Costs and runtimes varied a lot. GLM-5 was much cheaper than the other two large models, but was a lot slower and suffered from stability issues, requiring many retried requests.

And gpt-oss did fine for the most part, but really fell apart for antipatterns, which were more difficult to detect, and had a tendency to produce garbled responses, and this was especially evident for files, which required more reasoning.
-->

---

<style scoped>
h1 {
  font-size: 1.05em;
}

th, td {
  font-size: 0.8em;
}
</style>

# RQ2: How do different prompting strategies affect LLMs' accuracy in identifying SQL antipatterns in Java code using jOOQ for database access?

- 2% performance gains to non-reasoning model from CoT and ToT
- Up to 1% gains to open models from Few-Shot
- Gains for one model generally balanced by degradations for other models
- Large increases to cost and runtime

| Strategy         | Avg F1-Score Increase | Avg Cost Increase | Avg Runtime Increase |
| :--------------- | :-------------------: | :---------------: | :------------------: |
| Few-Shot         |         0.6%          |        31%        |         -1%          |
| Chain-of-Thought |         -0.3%         |        23%        |         90%          |
| Tree-of-Thought  |         0.1%          |        88%        |         638%         |

<!--
When testing the other prompting strategies, the gains over the baseline were modest at best. For example, the prompting strategies, which were used to elicit additional reasoning, increased the performance of the non-reasoning model, but slightly decreased the performance of reasoning models. And this came at a large cost - the analysis prices and runtimes increased significantly.

The Few-Shot prompting strategy slightly benefitted the open models, GLM-5 and gpt-oss, but increased analysis costs as well.
-->

---

# Analysis tool development and evaluation

- **Stack:** Bun (build tool & runtime), TypeScript, Vercel AI SDK.
- **Architecture:** Self-contained CLI tool
- **Pre-processing:**
  - Irrelevant files skipped based on black- and whitelists
  - **Line Number Prefixing:** Crucial to help LLMs maintain spatial awareness and prevent miscalculations
- Zero-Shot prompt, default model Claude Opus 4.5 (non-reasoning)
- **Output:** Human-readable text summaries + machine-readable JSON/CSV.

<!--
We implemented the learnings from our experiments as a full-featured analysis tool. We built it using the Bun JavaScript toolchain, with TypeScript as the programming language and Vercel's AI SDK as a means to communicate with different LLM providers. It is a built as a self-contained executable command-line tool, so it can be used without installing any external runtime like Java or Node, and does not require a graphical interface.

Some important things it does is that it skips the analysis on files, which are considered to be irrelevant based on their paths or contents, to avoid performing unnecessary model calls. It also prepends each line with its line number, which is necessary for LLMs to identify correct line numbers, as they struggled with it otherwise.

And that tool is capable of outputting both human-readable analysis results with explanations and refactoring suggestions, as well as machine readable formats for future integrations and evaluation purposes.
-->

---

<style scoped>
h1 {
  font-size: 1.2em;
}
</style>

# RQ3: How accurate is the developed LLM-based tool in detecting SQL antipatterns in Java code using jOOQ for database access?

- Overall Precision: **0.95** | Recall: **0.91** | F1-Score: **0.92**
- **Best Performers:** _Implicit Columns_ (0.97) and _ID Required_ (0.94)
- **Worst Performers:** _Keyless Entry_ (0.76) and _Fear of the Unknown_ (0.82)
  - **Why?** Require cross-file context

<!--
We evaluated the tool against the test set using the Claude Opus 4.5 model without reasoning, which gave us the best balance between performance and runtime, at the expense of cost, and the performance we saw from the tool was.. really good. The weighted average F1-score was better than any tool we compared it to. We noticed an overarching pattern that the tool performed the best on antipatterns, which were relatively self-contained, and struggled a bit more with ones that required an understanding of context from multiple files.
-->

---

<style scoped>
h1 {
  font-size: 1.3em;
}
</style>

# RQ4: What are the occurrence patterns of SQL antipatterns in Java code using jOOQ for database access?

- 15,931 total antipatterns found in 602 projects, 26.5 per project
- **Dominant Core:** "Implicit Columns" and "ID Required"
  - Contained by 89% and 87% of projects
  - High project-wise Jaccard Index (0.81) and Spearman correlation (0.43)
- **Missing Constraints:** "Keyless Entry" and "Beware of the Unknown"
  - High project-wise Jaccard Index (0.34) and Spearman correlation (0.32)
- **Infection Path:** Rarer SQL antipatterns are strong indicators of more common ones
  - High project-wise (0.95) and file-wise (0.54) conditional probability

<!--
Using the developed tool and the Claude Opus 4.5 model without reasoning, we analysed the complete set of 602 projects, and we found nearly 16 thousand antipattern occurrences, which equates to 26 and a half occurrences per project.

We found that "Implicit Columns" and "ID Required" were the most common antipatterns, found in 89 and 87 percent of projects, respectively. The "ID Required" antipattern indicates bad practices in creating primary keys for tables, such as non-descriptive names or redundant keys. These antipatterns also showed a very high degree of co-occurrence, and their numbers in projects grew proportionally.

There was also a visible pattern showing that the "Keyless Entry" and "Beware of the Unknown" pattern were highly correlated, indicating that projects, which are missing foreign key constraints, are also more likely to be missing NOT NULL constraints.

What we also found is that less common antipatterns are very good indicators that more common antipatterns are also present nearby. For example, if a file contains the "Poor Man's Search Engine" antipattern, there is a 54 percent percent chance that the "Implicit Columns" antipattern is present in the same file, and a 95 percent percent chance it's present in the project.
-->

---

<style scoped>
h1 {
  font-size: 1.5em;
}
</style>

# RQ5: Which jOOQ API methods are most frequently associated with SQL antipattern occurrences?

### Implicit Columns

- More than 75% associated with shorthands for fetching jOOQ records
  - e.g. `DSL.selectFrom(TABLE)`, `DSL.select().from(TABLE)`
- Only 12.9% explicitly show the intent to fetch all columns
  - e.g. `DSL.asterisk()`, `TABLE.fields()`

<!--
We found that over three quarters of all "Implicit Columns" occurrences were associated with jOOQ DSL methods, which are used to fetch records, which contain all columns of the underlying table. Although the jOOQ documentation discourages using such methods to avoid fetching redundant data, these are some of the simplest ways to fetch data using jOOQ, and are clearly still very popular. Perhaps there should be some other measures taken to discourage their use even further.

Methods, where the intent of fetching all columns was more explicit, were much less prevalent, at about thirteen percent of all occurrences.
-->

---

<style scoped>
h1 {
  font-size: 1.5em;
}
</style>

# RQ5: Which jOOQ API methods are most frequently associated with SQL antipattern occurrences?

### Poor Man's Search Engine

- More than 60% explicitly use `LIKE` and `ILIKE` operators with wildcards
  - `FIELD.like("%" + value + "%")`, `FIELD.ilike`
- Wildcards are added implicitly in 28% of cases
  - `FIELD.contains(value)`, `FIELD.containsIgnoreCase`

<!--
In case of the "Poor Man's Search Engine", the results were somewhat the opposite. This antipattern indicates that the database is being used as a full-text search engine, which does not scale well. The majority of sixty percent of cases of the antipattern used methods, which required the users to explicitly use preceding and succeeding wildcards for full-text search. And only less than thirty percent used methods, where the wildcards were added implicitly.
-->

---

# Highlights & Contributions

- Created a high-quality ground truth
- First to treat LLM-based antipattern detection as a localisation task
- Developed the first available tool for detecting SQL antipatterns in jOOQ
- Performed a large-scale analysis of jOOQ projects

<!--
So, what were the main contributions of this thesis? We created a high quality dataset about SQL antipattern occurrences in jOOQ projects. We analysed the current state of LLM performance in detecting SQL antipatterns, and we were the first to tackle a similar task as a localisation task. We developed the first available tool for detecting SQL antipatterns in jOOQ, and we used the tool to perform a large-scale analysis of jOOQ projects regarding the occurrence patterns of SQL antipatterns.
-->

---

# Thank you for listening!

<!--
Thank you for listening. I'm looking forward to your questions.
-->
