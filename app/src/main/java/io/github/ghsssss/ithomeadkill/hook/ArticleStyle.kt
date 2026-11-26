package io.github.ghsssss.ithomeadkill.hook

/** Exact selectors from the bundled IT之家 9.55 article templates, not keyword CSS. */
internal object ArticleStyle {
    fun script(hideSummary: Boolean, hideRating: Boolean, hideRewards: Boolean = false): String {
        val rules = buildList {
            if (hideSummary) add(".ai-abstract { display: none !important; }")
            if (hideRating) add("#userGrade, #editorGrade, .footer > .news_grade { display: none !important; }")
            if (hideRewards) add("body > .reward { display: none !important; }")
        }.joinToString("\\n")
        return """
            (function () {
                var body = document.getElementById('post_body');
                var oldGrade = document.getElementById('userGrade');
                if (!body && !oldGrade) return JSON.stringify({article:false});
                var style = document.getElementById('ithome-adkill-article-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'ithome-adkill-article-style';
                    (document.head || document.documentElement).appendChild(style);
                }
                var css = '$rules';
                if (style.textContent !== css) style.textContent = css;
                var summary = document.querySelector('.ai-abstract');
                var ratings = Array.prototype.slice.call(document.querySelectorAll('#userGrade, #editorGrade, .footer > .news_grade'));
                var reward = document.querySelector('body > .reward');
                var redpacket = reward && reward.querySelector('#redpacketWrap');
                var rewardHidden = !!reward && getComputedStyle(reward).display === 'none';
                return JSON.stringify({article:true, summary:!!summary,
                    summaryHidden:!!summary && getComputedStyle(summary).display === 'none',
                    summaryRequested:$hideSummary, ratingRequested:$hideRating,
                    rewardsRequested:$hideRewards, rewards:!!reward, rewardsHidden:rewardHidden,
                    redpacket:!!redpacket,
                    redpacketHidden:!!redpacket && (rewardHidden || getComputedStyle(redpacket).display === 'none'),
                    rating:ratings.length > 0,
                    ratingHidden:ratings.length > 0 && ratings.every(function (node) { return getComputedStyle(node).display === 'none'; })});
            })()
        """.trimIndent()
    }
}
