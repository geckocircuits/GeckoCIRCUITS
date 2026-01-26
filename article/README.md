# GeckoCIRCUITS Content Marketing System

**Purpose:** Token-efficient, deterministic content creation system for documenting the GeckoCIRCUITS refactoring journey and generating business opportunities.

**Target Audience:** Power electronics R&D professionals
**Business Goals:** Subscriptions, consulting leads, hiring, product adoption
**Content Types:** LinkedIn posts (300-500 words) + Substack articles (1,500-3,000 words)

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [System Overview](#system-overview)
3. [Skills](#skills)
4. [Tools](#tools)
5. [Workflows](#workflows)
6. [Directory Structure](#directory-structure)
7. [Content Calendar](#content-calendar)
8. [Examples](#examples)
9. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Create Your First LinkedIn Post

```bash
# Step 1: Extract metrics from a source file
python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md > metrics.json

# Step 2: Use the linkedin-writer skill
# In Claude Code CLI:
# "Using the linkedin-writer skill, write LinkedIn post #3 about REST API delivery.
#  Use metrics from metrics.json."

# Step 3: Review and publish
```

### Create Your First Substack Article

```bash
# Step 1: Extract metrics
python article/tools/metrics_extractor.py STRATEGIC_ROADMAP_DUAL_TRACK.md > metrics.json

# Step 2: Use the content-strategist skill to create outline
# "Using content-strategist skill, create detailed outline for Article #1 (Dual-Track Strategy)"

# Step 3: Use the substack-article-writer skill
# "Using substack-article-writer skill, write Article #1 based on the outline"

# Step 4: Review and publish to Substack
```

---

## System Overview

This content marketing system consists of:

### 1. Three Skills (`.claude/skills/`)

**Skills** are specialized AI agents that help you create content:
- `content-strategist.md` - Analyzes docs, creates outlines, plans content calendar
- `linkedin-writer.md` - Writes engaging 300-500 word LinkedIn posts
- `substack-article-writer.md` - Writes technical 1,500-3,000 word articles

### 2. Two Python Tools (`article/tools/`)

**Tools** extract metrics and render templates for token efficiency:
- `metrics_extractor.py` - Extracts quantitative data from markdown/git
- `template_renderer.py` - Fills content templates with metrics

### 3. SEO Keyword Cache (`article/tools/seo_keywords.json`)

Pre-researched keywords for power electronics domain to avoid repetition.

### 4. Content Calendar (`article/CONTENT_CALENDAR.md`)

15 LinkedIn posts + 7 Substack articles mapped to a 15-week publishing schedule.

---

## Skills

Skills are invoked using Claude Code's skill system. They provide specialized capabilities for content creation.

### Skill 1: Content Strategist

**Location:** `.claude/skills/content-strategist.md`

**Purpose:** High-level content strategy and planning

**Capabilities:**
- Analyze documentation files to extract key insights
- Create content calendars with publishing schedules
- Design content funnels (TOP → MIDDLE → BOTTOM)
- Research SEO keywords
- Map topics to audience pain points
- Compare competitive positioning (vs PLECS/MATLAB)

**When to Use:**
- Starting a new content series
- Need to map documentation to content topics
- Planning publishing schedule
- Researching keywords for a topic

**Example Usage:**
```
"Using the content-strategist skill, analyze PHASE_1_COMPLETE.md and suggest
3 LinkedIn post topics with hooks and CTAs."
```

**Available Tools in Skill:**
1. **DocumentAnalyzer** - Extract metrics and achievements from files
2. **TopicExtractor** - Find viral-worthy topics and narratives
3. **SEOOptimizer** - Research and recommend keywords
4. **FunnelDesigner** - Map content to buyer journey stages
5. **CompetitorAnalyzer** - Compare GeckoCIRCUITS vs PLECS/MATLAB

### Skill 2: LinkedIn Writer

**Location:** `.claude/skills/linkedin-writer.md`

**Purpose:** Write engaging LinkedIn posts optimized for algorithm

**Capabilities:**
- Write 300-500 word posts with strong hooks
- Format data for visual impact (bullets, arrows, comparisons)
- Create compelling CTAs matched to funnel stage
- Research relevant hashtags
- Add engagement boosters (questions, polls)

**When to Use:**
- Writing weekly LinkedIn posts
- Need to condense technical content for social media
- Want to drive traffic to Substack articles
- Building personal brand

**Example Usage:**
```
"Using the linkedin-writer skill, write LinkedIn post #6 about the -parameters
flag revelation. Use metrics from metrics.json. Target: MIDDLE funnel."
```

**Available Tools in Skill:**
1. **HookGenerator** - Create attention-grabbing opening lines
2. **DataHighlighter** - Format metrics for visual impact
3. **CTAOptimizer** - Craft compelling calls-to-action
4. **HashtagResearch** - Find relevant hashtags
5. **EngagementBooster** - Add questions/discussion prompts

### Skill 3: Substack Article Writer

**Location:** `.claude/skills/substack-article-writer.md`

**Purpose:** Write long-form technical articles with SEO optimization

**Capabilities:**
- Write 1,500-3,000 word technical case studies
- Structure: 7-section format (Executive Summary → CTA)
- Include code examples, architecture diagrams, data tables
- Generate SEO metadata (title tags, meta descriptions)
- Optimize for funnel stage (TOP/MIDDLE/BOTTOM)

**When to Use:**
- Writing bi-weekly Substack articles
- Need technical depth with practical lessons
- Want to demonstrate expertise
- Driving consulting leads

**Example Usage:**
```
"Using the substack-article-writer skill, write Article #3 (REST APIs on
Legacy Java) based on the outline in CONTENT_CALENDAR.md."
```

**Available Tools in Skill:**
1. **OutlineExpander** - Convert strategy outline to full structure
2. **CodeSnippetFormatter** - Format code with explanations
3. **DiagramDescriptor** - Describe architecture diagrams
4. **MetricsVisualizer** - Create tables/charts from data
5. **CTAFooter** - Add subscription/contact CTAs
6. **SEOMetadata** - Generate SEO-optimized metadata

---

## Tools

Python command-line tools for data extraction and template rendering.

### Tool 1: Metrics Extractor

**Location:** `article/tools/metrics_extractor.py`

**Purpose:** Extract structured metrics from documentation and git history

**Usage:**
```bash
# Extract from markdown file
python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md

# Extract from git log
python article/tools/metrics_extractor.py --git-log HEAD~10..HEAD

# Extract for specific sprint
python article/tools/metrics_extractor.py --sprint 15

# Output formats
python article/tools/metrics_extractor.py FILE.md --format json   # JSON (default)
python article/tools/metrics_extractor.py FILE.md --format yaml   # YAML
python article/tools/metrics_extractor.py FILE.md --format pretty # Human-readable
```

**Output Example:**
```json
{
  "tests_added": 93,
  "coverage_before": "0%",
  "coverage_after": "55%",
  "coverage_gain": "+55.0pp",
  "sprint_number": 15,
  "source_file": "SESSION_SUMMARY_2026-01-25.md",
  "title": "Sprint 15: Math Package Test Coverage",
  "key_achievements": [
    "Added 93 tests to math package",
    "Improved coverage from 0% to 55%",
    "Created 5 test files"
  ]
}
```

**Token Savings:** ~500 tokens per article by providing structured data

### Tool 2: Template Renderer

**Location:** `article/tools/template_renderer.py`

**Purpose:** Fill content templates with extracted metrics

**Usage:**
```bash
# Render a template with metrics file
python article/tools/template_renderer.py linkedin_achievement metrics.json

# Use stdin
echo '{"tests_added": 93}' | python article/tools/template_renderer.py linkedin_achievement -

# Add extra variables
python article/tools/template_renderer.py linkedin_coverage metrics.json \
  --var "hook=Amazing progress!" \
  --var "cta=Follow for more insights"

# List all available templates
python article/tools/template_renderer.py --list-templates

# Save to file
python article/tools/template_renderer.py article_executive_summary metrics.json -o output.md
```

**Available Templates:**
- `linkedin_achievement` - Post celebrating an achievement
- `linkedin_coverage` - Post about test coverage improvements
- `linkedin_pivot` - Post about strategic pivot
- `linkedin_technical` - Post with code snippet
- `article_executive_summary` - Article executive summary section
- `article_problem` - Article problem statement section
- `article_results` - Article results section
- `article_lessons` - Article lessons learned section
- `metrics_table` - Markdown table for metrics
- `before_after` - Before/after comparison
- `bullet_list` - Simple bullet list

**Token Savings:** ~300 tokens per post by using templates

### Tool 3: SEO Keywords Cache

**Location:** `article/tools/seo_keywords.json`

**Purpose:** Pre-researched keywords to avoid repetition

**Structure:**
```json
{
  "primary_keywords": [...],      // High-volume, competitive
  "secondary_keywords": [...],    // Medium-volume, targetable
  "technical_keywords": [...],    // Industry-specific
  "pain_point_keywords": [...],   // Problem-focused
  "long_tail_keywords": [...],    // Low-volume, high-intent
  "linkedin_hashtags": {...},     // Organized by category
  "content_templates": {...}      // Recommended combinations
}
```

**Usage:**
```bash
# View keywords
cat article/tools/seo_keywords.json | jq '.primary_keywords'

# Get hashtags for LinkedIn
cat article/tools/seo_keywords.json | jq '.linkedin_hashtags.software_engineering'

# Get keyword template for specific content
cat article/tools/seo_keywords.json | jq '.content_templates.rest_api_article'
```

**Token Savings:** ~200 tokens per article by avoiding keyword research

---

## Workflows

### Workflow 1: Write a LinkedIn Post

**Goal:** Create a 300-500 word LinkedIn post in 15 minutes

**Steps:**

1. **Check the calendar** (`article/CONTENT_CALENDAR.md`)
   - Identify which post to write (e.g., LinkedIn Post #3)
   - Note: Topic, funnel stage, target audience, source files

2. **Extract metrics** (optional but recommended)
   ```bash
   python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md > metrics.json
   ```

3. **Use linkedin-writer skill**
   ```
   "Using the linkedin-writer skill, write LinkedIn post #3:
   - Topic: 23 REST Endpoints in 2 Weeks
   - Source: PHASE_1_COMPLETE.md
   - Funnel stage: TOP (Awareness)
   - Include metrics from metrics.json
   - Add hook, data points, and CTA to read Substack article"
   ```

4. **Review the output**
   - Check word count (300-500)
   - Verify hook is strong
   - Ensure data is included
   - Confirm CTA matches funnel stage
   - Validate hashtags (5-7)

5. **Save to drafts**
   ```bash
   # Save output to file
   cat > article/linkedin/drafts/post-03-rest-endpoints.md
   ```

6. **Edit and finalize**
   - Human review required
   - Adjust tone if needed
   - Fix any errors

7. **Publish to LinkedIn**
   - Copy to LinkedIn
   - Schedule or publish immediately
   - Track metrics (engagement rate, clicks)

8. **Move to published**
   ```bash
   mv article/linkedin/drafts/post-03-rest-endpoints.md \
      article/linkedin/published/
   ```

**Time Estimate:** 15-20 minutes

### Workflow 2: Write a Substack Article

**Goal:** Create a 1,500-3,000 word technical article in 60-90 minutes

**Steps:**

1. **Check the calendar** (`article/CONTENT_CALENDAR.md`)
   - Identify which article to write (e.g., Article #3)
   - Note: Topic, structure, sections, word counts, source files

2. **Extract metrics from multiple sources**
   ```bash
   python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md > phase1_metrics.json
   python article/tools/metrics_extractor.py SESSION_SUMMARY_2026-01-25.md > session_metrics.json
   ```

3. **Use content-strategist skill for outline** (if not already in calendar)
   ```
   "Using the content-strategist skill, create a detailed outline for
   Article #3 (Building Modern REST APIs on Legacy Java):
   - Expand each section with subsections
   - Identify code examples needed
   - Plan data tables and diagrams
   - Map to source files"
   ```

4. **Use substack-article-writer skill**
   ```
   "Using the substack-article-writer skill, write Article #3 based on
   the outline in CONTENT_CALENDAR.md:
   - Target audience: Backend engineers modernizing systems
   - Funnel stage: MIDDLE (Consideration)
   - Include code examples for Maven setup and -parameters flag
   - Add data table for Phase 1 deliverables
   - Describe WebSocket architecture
   - Generate SEO metadata"
   ```

5. **Review the output**
   - Check word count (1,500-3,000)
   - Verify all 7 sections present
   - Ensure code examples are formatted
   - Confirm data tables are clear
   - Validate SEO metadata

6. **Save to drafts**
   ```bash
   cat > article/substack/drafts/article-03-rest-api-legacy-java.md
   ```

7. **Edit and enhance**
   - Human review required
   - Add any missing details
   - Verify code examples compile
   - Check for typos/grammar
   - Ensure flow is smooth

8. **Generate supporting assets** (if needed)
   - Architecture diagrams (use tools like draw.io)
   - Screenshots
   - Code examples in separate files

9. **Publish to Substack**
   - Copy to Substack editor
   - Add images/diagrams
   - Set SEO metadata (title tag, meta description)
   - Preview on mobile
   - Schedule or publish immediately

10. **Move to published**
    ```bash
    mv article/substack/drafts/article-03-rest-api-legacy-java.md \
       article/substack/published/
    ```

11. **Cross-promote**
    - Share on LinkedIn (see LinkedIn post #6)
    - Link from related posts
    - Add to email newsletter

**Time Estimate:** 60-90 minutes

### Workflow 3: Plan a Content Series

**Goal:** Map documentation to content calendar

**Steps:**

1. **Identify source materials**
   ```bash
   ls *.md | grep -E "(PHASE|SPRINT|SESSION|STRATEGIC)"
   ```

2. **Use content-strategist skill**
   ```
   "Using the content-strategist skill, analyze the following files:
   - PHASE_1_COMPLETE.md
   - BACKEND_COVERAGE_PLAN.md
   - STRATEGIC_ROADMAP_DUAL_TRACK.md

   Create a content strategy with:
   - 5 LinkedIn post topics (with hooks and CTAs)
   - 2 Substack article outlines (with sections)
   - SEO keywords for each
   - Funnel stage mapping (TOP/MIDDLE/BOTTOM)"
   ```

3. **Extract metrics from all sources**
   ```bash
   for file in PHASE_*.md SPRINT*.md SESSION*.md; do
     python article/tools/metrics_extractor.py "$file" > "metrics_$(basename $file .md).json"
   done
   ```

4. **Review strategy output**
   - Verify topics are relevant
   - Check funnel progression
   - Ensure variety (quick wins + deep dives)

5. **Update content calendar**
   - Add new topics to `CONTENT_CALENDAR.md`
   - Map source files
   - Set publishing dates

6. **Create outlines directory**
   ```bash
   mkdir -p article/tools/outlines
   # Save outlines from strategy
   ```

**Time Estimate:** 45-60 minutes

### Workflow 4: Track Performance

**Goal:** Measure content effectiveness and adjust strategy

**Steps:**

1. **Weekly: Track LinkedIn metrics**
   - Engagement rate (likes + comments + shares / impressions)
   - Follower growth
   - Click-through rate to Substack
   - Top-performing posts

2. **Bi-weekly: Track Substack metrics**
   - New subscribers
   - Open rate
   - Time on page
   - Scroll depth
   - Top-performing articles

3. **Monthly: Track business metrics**
   - Consulting inquiries (from LinkedIn DMs, Substack emails)
   - Consultation bookings
   - Revenue generated
   - Hiring opportunities

4. **Quarterly: Adjust strategy**
   - What's working? Do more of it.
   - What's not working? Try new approaches.
   - Update content calendar based on data
   - Refine CTAs for better conversion

**Tools:**
- LinkedIn Analytics (native)
- Substack Analytics (native)
- Google Analytics (if website)
- Spreadsheet for business metrics

---

## Directory Structure

```
article/
├── README.md                          # This file - usage guide
├── CONTENT_CALENDAR.md                # 15 LinkedIn posts + 7 articles
│
├── tools/                             # Python tools for content creation
│   ├── metrics_extractor.py           # Extract metrics from docs/git
│   ├── template_renderer.py           # Fill templates with metrics
│   ├── seo_keywords.json              # Pre-researched keywords
│   └── outlines/                      # Approved article outlines
│       ├── article-01-dual-track.md
│       ├── article-02-test-coverage.md
│       └── ...
│
├── linkedin/                          # LinkedIn posts
│   ├── drafts/                        # Work in progress
│   │   ├── post-01-desktop-migration.md
│   │   └── ...
│   └── published/                     # Published posts
│       ├── 2026-02-01-post-01.md
│       └── ...
│
├── substack/                          # Substack articles
│   ├── drafts/                        # Work in progress
│   │   ├── article-01-dual-track.md
│   │   └── ...
│   └── published/                     # Published articles
│       ├── 2026-02-05-article-01.md
│       └── ...
│
└── assets/                            # Supporting files
    ├── diagrams/                      # Architecture diagrams
    ├── screenshots/                   # UI screenshots
    └── code-examples/                 # Standalone code files
```

---

## Content Calendar

See `article/CONTENT_CALENDAR.md` for the complete 15-week schedule:

- **15 LinkedIn Posts:** Weekly publishing, 300-500 words each
- **7 Substack Articles:** Bi-weekly publishing, 1,500-3,000 words each
- **Funnel Progression:** TOP (Weeks 1-3) → MIDDLE (Weeks 4-7) → BOTTOM (Weeks 8-15)

**Quick Reference:**

| Week | LinkedIn | Substack | Stage |
|------|----------|----------|-------|
| 1 | #1: Desktop Migration Kill | - | TOP |
| 2 | #2: Coverage Journey | Article #1: Dual-Track | TOP |
| 3 | #3: 23 Endpoints | - | TOP |
| 4 | #4: God Classes | Article #2: Test Coverage | MIDDLE |
| 6 | #6: Spring Boot Legacy | Article #3: REST APIs | MIDDLE |
| 8 | #8: Open Source vs Commercial | Article #4: Interface Extraction | BOTTOM |
| ... | ... | ... | ... |

---

## Examples

### Example 1: Extract Metrics and Create LinkedIn Post

```bash
# Extract metrics
$ python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md --format pretty

# Metrics: Phase 1: REST API Backend Complete

**Summary:** Phase 1 deliverables completed: 23 REST endpoints, Spring Boot integration, STOMP WebSocket configuration, real-time simulation updates...

**Endpoints Created:** 23
**Duration:** 2 weeks
**Technologies:** Spring Boot, Maven, REST API, WebSocket, STOMP, Java

**Key Achievements:**
  • 23 REST endpoints delivered in 2 weeks
  • Spring Boot integrated with legacy core
  • STOMP WebSocket for real-time updates

# Now use this data in linkedin-writer skill
# "Using linkedin-writer skill, write LinkedIn post #3 using the metrics above"
```

### Example 2: Use Template Renderer

```bash
# Create metrics file
$ cat > metrics.json <<EOF
{
  "tests_added": 93,
  "coverage_before": "0%",
  "coverage_after": "55%",
  "coverage_gain": "+55pp",
  "sprint_number": 15,
  "duration": "2 weeks"
}
EOF

# Render LinkedIn post template
$ python article/tools/template_renderer.py linkedin_coverage metrics.json \
  --var "context=We focused on the math package, which had zero tests." \
  --var "lesson=Sometimes the biggest wins come from tackling the scariest packages." \
  --var "cta=Follow for more test coverage insights." \
  --var "hashtags=#TestDrivenDevelopment #LegacyCode #Java"

# Output: Formatted LinkedIn post ready to review and publish
```

### Example 3: Full Article Workflow

```bash
# Step 1: Extract metrics from multiple sources
python article/tools/metrics_extractor.py PHASE_1_COMPLETE.md > phase1.json
python article/tools/metrics_extractor.py BACKEND_COVERAGE_PLAN.md > coverage.json

# Step 2: Use content-strategist skill
# "Using content-strategist skill, create outline for Article #2 (Test Coverage)"

# Step 3: Save outline to file
mkdir -p article/tools/outlines
# Save output to article/tools/outlines/article-02-test-coverage.md

# Step 4: Use substack-article-writer skill
# "Using substack-article-writer skill, write Article #2 based on outline"

# Step 5: Save draft
# Save output to article/substack/drafts/article-02-test-coverage.md

# Step 6: Review, edit, publish
# ... human review and editing ...

# Step 7: Move to published
mv article/substack/drafts/article-02-test-coverage.md \
   article/substack/published/2026-02-12-test-coverage-guide.md
```

---

## Troubleshooting

### Problem: Metrics Extractor Returns Empty Data

**Cause:** File format doesn't match expected patterns

**Solution:**
1. Check file contains metrics (numbers, percentages)
2. Use `--format pretty` to see what was extracted
3. Manually add missing data to JSON output
4. Update extraction patterns in `metrics_extractor.py` if needed

### Problem: Skills Not Available

**Cause:** Skills not loaded by Claude Code

**Solution:**
1. Verify files exist: `ls .claude/skills/*.md`
2. Restart Claude Code session
3. Manually reference skill file if needed

### Problem: LinkedIn Post Too Long

**Cause:** Content exceeded 500-word limit

**Solution:**
1. Use linkedin-writer skill with explicit length constraint
2. Remove less critical details
3. Save full version for Substack article
4. Focus on hook + key data + CTA

### Problem: Substack Article Lacks Technical Depth

**Cause:** Not enough code examples or architecture details

**Solution:**
1. Review source files for code snippets
2. Use CodeSnippetFormatter tool in substack-article-writer skill
3. Add more examples in Implementation section
4. Describe architecture diagrams with DiagramDescriptor

### Problem: Low Engagement on LinkedIn

**Cause:** Weak hook, wrong posting time, or poor CTA

**Solution:**
1. A/B test different hooks (use HookGenerator)
2. Post during peak times (Tuesday-Thursday, 8-10 AM)
3. Engage with comments within first hour
4. Adjust CTA to be more compelling

### Problem: No Consulting Leads

**Cause:** BOTTOM funnel CTAs not strong enough

**Solution:**
1. Add more explicit CTAs in BOTTOM articles
2. Offer free 30-minute audits
3. Include case study/testimonial
4. Make contact process easier (link in profile)

---

## Token Efficiency

This system is designed for token efficiency:

| Component | Token Savings | How |
|-----------|---------------|-----|
| Metrics Extractor | ~500 per article | Structured JSON instead of re-reading full docs |
| Template Renderer | ~300 per post | Fill templates instead of regenerating content |
| SEO Keyword Cache | ~200 per article | Pre-researched keywords, no repetition |
| Content Outlines | ~1000 per article | Approved outlines, no regeneration |
| **Total Savings** | **~2000 tokens per article** | **50%+ reduction in token usage** |

---

## Success Metrics

### LinkedIn Goals (3 Months)
- **Follower Growth:** +200 followers
- **Engagement Rate:** >5% average
- **Click-Through Rate:** >2% to Substack

### Substack Goals (3 Months)
- **Subscribers:** 100 subscribers
- **Open Rate:** >40%
- **Time on Page:** 7+ minutes

### Business Goals (3 Months)
- **Consulting Inquiries:** 5 qualified leads
- **Consultations Booked:** 2 paid consultations
- **Revenue Generated:** €6,000+ (2x ROI)

---

## Support & Feedback

**Questions?**
- Review this README
- Check CONTENT_CALENDAR.md for details
- Consult skill files for tool usage

**Found a Bug?**
- File issue with details
- Include error message and command used

**Suggestions?**
- New template ideas for template_renderer.py
- Additional metrics for metrics_extractor.py
- Improvements to skills

---

**Last Updated:** 2026-01-26
**Version:** 1.0
**Status:** Ready for use

**Next Steps:**
1. Read CONTENT_CALENDAR.md
2. Write first LinkedIn post (Week 1)
3. Start first Substack article (Week 2)
4. Track metrics weekly
5. Adjust strategy based on results

Good luck with your content marketing! 🚀
