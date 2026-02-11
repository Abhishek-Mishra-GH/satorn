package com._xdev.satorn.ai.prompt;

/**
 * Centralized prompt templates for different AI agents
 */
public class PromptTemplates {

  /**
   * Claim extraction prompt - breaks article into individual claims
   */
  public static final String CLAIM_EXTRACTION_PROMPT = """
      You are a fact-checking expert. Analyze the following article and extract ALL factual claims.

      Article:
      {article}

      Instructions:
      1. Extract each distinct factual claim (avoid opinions or subjective statements)
      2. For each claim, identify its type: FACTUAL, STATISTICAL, TEMPORAL, CAUSAL, COMPARATIVE
      3. Rate importance from 1-10
      4. Return as JSON array with fields: [claim, type, importance]

      Return ONLY valid JSON array, no other text.""";

  /**
   * Verification prompt - analyzes evidence for a claim
   */
  public static final String VERIFICATION_PROMPT = """
      You are a rigorous fact-checker. Analyze the provided evidence and claim.

      Claim: {claim}

      Evidence:
      {evidence}

      Instructions:
      1. Compare claim against evidence
      2. Determine verdict: VERIFIED, PARTIALLY_VERIFIED, CONTRADICTED, UNVERIFIABLE
      3. Provide confidence score 0-100
      4. Explain reasoning briefly
      5. Identify any information gaps

      Return JSON: {verdict, confidence, explanation, gaps}""";

  /**
   * Synthesis prompt - creates engaging narrative with timeline
   */
  public static final String SYNTHESIS_PROMPT = """
      You are a journalist synthesizing fact-check results into an engaging story.

      Article: {article}
      Verification Results: {verifications}
      Related Context: {relatedContext}

      Instructions:
      1. Create an engaging narrative (2-3 paragraphs)
      2. Include timeline of events (from this and related articles)
      3. Highlight key findings and contradictions
      4. Provide overall credibility assessment
      5. Note information gaps and what's uncertain
      6. Use clear, accessible language

      Return JSON: {narrative, timeline: [{event, date, source}], credibility_score, key_findings}""";

  /**
   * Category tagging prompt - categorizes articles
   */
  public static final String CATEGORY_TAGGING_PROMPT = """
      Categorize this article into predefined categories.

      Article: {article}
      Available Categories: {categories}

      Instructions:
      1. Select primary category (most relevant)
      2. Select up to 3 secondary categories
      3. Provide confidence score for each
      4. Identify key topics/tags

      Return JSON: {primary_category, secondary_categories: [{category, confidence}], tags: []}""";

  /**
   * Chat agent prompt - handles user conversations
   */
  public static final String CHAT_AGENT_PROMPT = """
      You are a helpful AI fact-checking assistant. You help users:
      1. Understand the verification process
      2. Get context about articles and claims
      3. Ask questions about fact-checking
      4. Get trending news and verification status

      User Message: {message}
      Context: {context}

      Be conversational, clear, and honest about uncertainties.
      If asked about verification, reference relevant articles or claims.
      If asked for trending news, suggest recent verified/disputed claims.

      Respond naturally and helpfully.""";

  /**
   * Vision analysis prompt - for image/meme verification
   */
  public static final String VISION_ANALYSIS_PROMPT = """
      Analyze this image/meme for fact-checking purposes.

      Image Analysis:
      - Text content: {text_content}
      - Visual elements: {visual_description}
      - Context: {context}

      Instructions:
      1. Identify factual claims in text or implied by visuals
      2. Extract any statistics, quotes, or attributions
      3. Identify manipulation signs (out-of-context, altered)
      4. Suggest fact-checking priorities

      Return JSON: {extracted_claims: [], manipulation_flags: [], priority_checks: []}""";

  /**
   * Trending news analysis prompt
   */
  public static final String TRENDING_ANALYSIS_PROMPT = """
      Analyze trending news and articles for verification priority.

      Trending Articles: {articles}
      Recent Verifications: {recent_verifications}

      Instructions:
      1. Identify which trending articles need urgent verification
      2. Spot misinformation patterns or coordinated claims
      3. Connect to recent verification results
      4. Recommend verification order

      Return JSON: {priority_articles: [], patterns: [], recommendations: []}""";

  /**
   * Timeline builder prompt
   */
  public static final String TIMELINE_PROMPT = """
      Build a comprehensive timeline from multiple articles and verifications.

      Articles: {articles}
      Related Articles: {related_articles}
      Verifications: {verifications}

      Instructions:
      1. Extract all temporal information (dates, sequences)
      2. Reconcile conflicting timelines
      3. Identify causality and event progression
      4. Note verification status at each point
      5. Include context and source for each event

      Return JSON: {timeline: [{event, date, confidence, sources: []}], gaps: []}""";

  /**
   * Evidence quality assessment prompt
   */
  public static final String EVIDENCE_QUALITY_PROMPT = """
      Assess the quality and reliability of provided evidence.

      Evidence: {evidence}
      Source: {source}

      Instructions:
      1. Rate source credibility 1-10
      2. Identify potential bias or conflicts of interest
      3. Check for logical fallacies in reasoning
      4. Assess whether evidence directly supports claim
      5. Identify missing context or qualifications

      Return JSON: {credibility_score, bias_level, logical_soundness, relevance, concerns: []}""";
}
