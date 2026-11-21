package io.github.ghsssss.ithomeadkill.hook

import org.junit.Assert.*
import org.junit.Test

class ArticleStyleTest {
    @Test fun disabledTogglesReplacePreviousCssWithEmptyRules() {
        val script = ArticleStyle.script(false, false)
        assertTrue(script.contains("var css = '';"))
        assertTrue(script.contains("style.textContent = css"))
    }

    @Test fun summaryRuleDoesNotHideArticleBodyOrAskAiButtons() {
        val script = ArticleStyle.script(true, false)
        assertTrue(script.contains(".ai-abstract { display: none !important; }"))
        assertFalse(script.contains("#post_body {"))
        assertFalse(script.contains(".content {"))
        assertFalse(script.contains(".ask-ai-btn {"))
        assertFalse(script.contains("#userGrade, #editorGrade, .footer > .news_grade {"))
    }

    @Test fun ratingRuleDoesNotTargetCommentVotesOrUserGrades() {
        val script = ArticleStyle.script(false, true)
        assertTrue(script.contains("#userGrade, #editorGrade, .footer > .news_grade { display: none !important; }"))
        assertFalse(script.contains(".ai-abstract {"))
        assertFalse(script.contains(".user_grade {"))
        assertFalse(script.contains(".support {"))
        assertFalse(script.contains(".oppose {"))
    }

    @Test fun rewardsCloseOnlyTheRecognizedArticleRewardContainer() {
        val script = ArticleStyle.script(false, false, true)
        assertTrue(script.contains("body > .reward { display: none !important; }"))
        assertTrue(script.contains("rewardsRequested:true"))
        assertFalse(script.contains(".ai-abstract {"))
        assertFalse(script.contains("#userGrade, #editorGrade, .footer > .news_grade {"))
        assertFalse(script.contains("#post_body {"))
        assertFalse(script.contains(".login-tip {"))
        assertFalse(script.contains(".comment {"))
    }

    @Test fun disabledRewardsDoNotChangeOtherRulesAndAllOffClearsCss() {
        val existing = ArticleStyle.script(true, true, false)
        assertTrue(existing.contains(".ai-abstract { display: none !important; }"))
        assertTrue(existing.contains("#userGrade, #editorGrade, .footer > .news_grade { display: none !important; }"))
        assertFalse(existing.contains("body > .reward {"))
        assertTrue(ArticleStyle.script(false, false, false).contains("var css = '';"))
        assertTrue(ArticleStyle.script(false, false, false).contains("rewardsRequested:false"))
    }

    @Test fun rewardsStatusChecksParentAndRedpacketWithoutReturningArticleContent() {
        val script = ArticleStyle.script(false, false, true)
        assertTrue(script.contains("document.querySelector('body > .reward')"))
        assertTrue(script.contains("rewardsHidden:"))
        assertTrue(script.contains("redpacketHidden:"))
        assertFalse(script.contains("innerText"))
        assertFalse(script.contains("outerHTML"))
    }

    @Test fun scriptIsScopedToRecognizedArticleTemplates() {
        val script = ArticleStyle.script(true, true)
        assertTrue(script.contains("if (!body && !oldGrade) return"))
        assertTrue(script.contains("summaryRequested:true"))
        assertTrue(script.contains("ratingRequested:true"))
        assertTrue(script.contains("ithome-adkill-article-style"))
    }
}
