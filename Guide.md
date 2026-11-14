# Index

- [What is Markdown?](#what-is-markdown)
- [What is CommonMark?](#what-is-commonmark)
- [What is GitHub Flavored Markdown?](#what-is-github-flavored-markdown)
- [What is LaTeX?](#what-is-latex)
- [How to use LaTeX in Markdown?](#how-to-use-latex-in-markdown)
- [What is OmniPad?](#what-is-omnipad-approach)
- [What has OmniPad offered?](#what-has-omnipad-offered)
- [Some Basic Markdown Syntax](#some-basic-markdown-syntax)
- [Some Extended Markdown Syntax](#some-extended-markdown-syntax)
- [LaTeX Math Syntax](#latex-math-syntax)
- [Greek Letters](#greek-letters)
- [References](#references)

# What is Markdown?

Markdown is a plain text format for writing structured documents, based on conventions for
indicating formatting in email and usenet posts.
It was developed by John Gruber (with help from Aaron Swartz) and released in 2004 in the form of a
syntax description and a Perl script (Markdown.pl) for converting Markdown to HTML.

In the next decade, dozens of implementations were developed in many languages.
Some extended the original Markdown syntax with conventions for footnotes, tables, and other
document elements.
Some allowed Markdown documents to be rendered in formats other than HTML.

Websites like Reddit, StackOverflow, and GitHub had millions of people using Markdown.
And Markdown started to be used beyond the web, to author books, articles, slide shows, letters, and
lecture notes.

# What is CommonMark?

CommonMark was developed to address inconsistencies and ambiguities in Markdown.

The CommonMark specification defines rules for elements such as headings, lists, links, emphasis,
and code blocks, among others.

By adhering to the CommonMark standard, developers can ensure consistent rendering of Markdown
content across different applications and platforms.

# What is GitHub Flavored Markdown?

GitHub Flavored Markdown, often shortened as GFM, is the dialect of Markdown that is currently
supported for user content on GitHub.com and GitHub Enterprise.

This formal specification, based on the CommonMark Spec, defines the syntax and semantics of this
dialect.

GFM is a strict superset of CommonMark. All the features which are supported in GitHub user content
and that are not specified on the original CommonMark Spec are hence known as extensions, and
highlighted as such.

While GFM supports a wide range of inputs, it’s worth noting that GitHub.com and GitHub Enterprise
perform additional post-processing and sanitization after GFM is converted to HTML to ensure
security and consistency of the website.

# What is LaTeX?

LaTeX is a typesetting system commonly used for producing scientific and mathematical documents.

LaTeX provides a powerful way to represent mathematical notation in documents, allowing users to
create complex equations and formulas with ease.

# How to use LaTeX in Markdown?

Mathematical expressions are key to information sharing amongst engineers, scientists, data
scientists, and mathematicians.

You can use the $ and $$ delimiters in GFM to insert math expressions in TeX and LaTeX style syntax.

# What is OmniPad Approach

OmniPad is built around the idea that information should be organized based on context, intent, and workflow rather than being confined to a single linear note system.

At the core of this approach is the concept of **Workspaces**.

A workspace acts as an independent environment that users can create based on specific needs or domains of their life or work. For example, a user might create separate workspaces for Personal Life, Work Projects, Research, Study, or Fitness Tracking.

Each workspace functions as a self-contained organizational unit, allowing users to keep related data structured and isolated without interference from other contexts.

Within every workspace, OmniPad provides multiple **Templates** designed for different types of information and productivity workflows. These templates include, but are not limited to:

* **Notes** — Rich Markdown documents with media and LaTeX support
* **Todo** — Task tracking with completion states and prioritization
* **Events** — Scheduled items with time and reminder support
* **Calendar** — Temporal visualization of events and plans
* **Journal** — Chronological personal or professional entries
* **Analytics** — Insights and productivity metrics derived from workspace data

Tap a calendar day to see its timeline. Timed events can have an optional end time,
including on the following day; events without an end time appear as 30-minute
blocks. All-day events appear above the timeline.

This modular structure allows users to mix and match templates depending on their requirements. A research workspace might emphasize notes and analytics, while a personal workspace may focus on journals, todos, and events.

By separating data into workspaces and structuring content through templates, OmniPad enables scalable organization, reduces cognitive clutter, and supports diverse productivity systems within a single unified platform.

# What has OmniPad offered?

OmniPad supports CommonMark and GitHub Flavored Markdown (GFM) syntax, as well as LaTeX math
syntax.

This allows users to create richly formatted notes with support for headings, lists, links,
emphasis, code blocks, tables, and mathematical expressions, and allows notes to be exported in
various formats including TXT, MD (Markdown), and HTML.

Use the document attachment button in a note or journal editor to add a PDF or another file type.
PDFs show an inline preview of every page and open the full document when tapped; other files appear
as links that open with a compatible app. OmniPad stores copied attachments in `OmniPad/Documents`.
Events can also have files attached from their details screen; tap an attachment name to open it
with the app associated with that file type.
Full backups include document attachments in the `media/documents` folder and restore them to
`OmniPad/Documents`. Document and PDF links from the original application remain usable after
restoring a backup.

# Some Basic Markdown Syntax

|    Element     |               Syntax               |
|:--------------:|:----------------------------------:|
|    Heading     | `# H1` <br/> `## H2`<br/> `### H3` |
|     Italic     |       `_italic_ or *italic*`       |
|      Bold      |       `**bold** or __bold__`       |
|   Blockquote   |           `> Blockquote`           |
|      Link      | `[title](https://www.example.com)` |
|      Code      |            `` `code` ``            |
|  Ordered List  | `1. List item 1 or 2) List item 2` |
| Unordered List | `- Apple or + Banana or * Orange`  |
|     Image      |     `![alt text](image.jpeg)`      |

# Some Extended Markdown Syntax

|    Element    |               Syntax               |
|:-------------:|:----------------------------------:|
| Strikethrough |        `~~Strikethrough~~`         |
|   Underline   |          `++Underline++`           |
|   Highlight   |           `==Marking==`            |
|   Task List   | `- [x] Task 1`<br/> `- [ ] Task 2` |
|  Heading ID   |      `# Heading {#custom-id}`      |

# LaTeX Math Syntax

|   Element   |       Syntax       |
|:-----------:|:------------------:|
|   Inline    |      `$x^2$`       |
|   Display   |     `$$x^2$$`      |
|   newline   |    `\\ or \\\\`    |
| Whitespace  |    `\quad or \`    |
| Superscript |       `x^2`        |
|  Subscript  |       `y_1`        |
| Expression  |       `{x}`        |
|  Overline   |   `\overline{x}`   |
|  Underline  |  `\underline{x}`   |
|  Fraction   |   `\frac{x}{y}`    |
| Left Paren  |      `\left(`      |
| Right Paren |     `\right)`      |
| Parentheses | `\left(x+y\right)` |
|  Brackets   | `\left[x+y\right]` |
|   Braces    | `\left{x+y\right}` |
|    Root     |   `\sqrt[n]{x}`    |
|      ×      |      `\times`      |
|      ÷      |       `\div`       |
|      ±      |       `\pm`        |
|      ≠      |       `\neq`       |
|      ≈      |     `\approx`      |
|      ≤      |       `\leq`       |
|      ≥      |       `\geq`       |
|      ∞      |      `\infty`      |
|      ∑      |       `\sum`       |
|      ∏      |      `\prod`       |
|      ∫      |       `\int`       |
|      ∑      |       `\sum`       |
|     lim     |       `\lim`       |
|      ∀      |     `\forall`      |
|      ∃      |     `\exists`      |
|      ∴      |    `\therefore`    |
|      ∵      |     `\because`     |
|      ⊂      |     `\subset`      |
|      ⊃      |     `\supset`      |
|      ⊆      |    `\subseteq`     |
|      ⊇      |    `\supseteq`     |
|      ∈      |       `\in`        |
|      ∉      |      `\notin`      |

# Greek Letters

| Uppercase |   Syntax   | Lowercase |   Syntax   |
|:---------:|:----------:|:---------:|:----------:|
|     A     |    `A`     |     α     |  `\alpha`  |
|     Β     |    `B`     |     β     |  `\beta`   |
|     Γ     |  `\Gamma`  |     γ     |  `\gamma`  |
|     Δ     |  `\Delta`  |     δ     |  `\delta`  |
|     Ε     |    `E`     |     ε     | `\epsilon` |
|     Ζ     |    `Z`     |     ζ     |  `\zeta`   |
|     Η     |    `H`     |     η     |   `\eta`   |
|     Θ     |  `\Theta`  |     θ     |  `\theta`  |
|     Ι     |    `I`     |     ι     |  `\iota`   |
|     Κ     |    `K`     |     κ     |  `\kappa`  |
|     Λ     | `\Lambda`  |     λ     | `\lambda`  |
|     Μ     |    `M`     |     μ     |   `\mu`    |
|     Ν     |    `N`     |     ν     |   `\nu`    |
|     Ξ     |   `\Xi`    |     ξ     |   `\xi`    |
|     Ο     |    `O`     |     ο     | `\omicron` |
|     Π     |   `\Pi`    |     π     |   `\pi`    |
|     Ρ     |    `P`     |     ρ     |   `\rho`   |
|     Σ     |  `\Sigma`  |     σ     |  `\sigma`  |
|     Τ     |    `T`     |     τ     |   `\tau`   |
|     Υ     | `\Upsilon` |     υ     | `\upsilon` |
|     Φ     |   `\Phi`   |     φ     |   `\phi`   |
|     Χ     |    `X`     |     χ     |   `\chi`   |
|     Ψ     |   `\Psi`   |     ψ     |   `\psi`   |
|     Ω     |  `\Omega`  |     ω     |  `\omega`  |

# References

You can find more information about Markdown, CommonMark, GitHub Flavored Markdown, LaTeX Math, and
Mermaid at the following links:

- [CommonMark](https://commonmark.org/)
- [GitHub Flavored Markdown](https://github.github.com/gfm/)
- [LaTeX](https://www.latex-project.org/)
- [Mermaid](https://mermaid.js.org/)