package hudson.plugins.redmine;

import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.util.regex.Pattern;

/**
 * Annotates <a href="http://www.redmine.org/wiki/redmine/RedmineSettings#Referencing-issues-in-commit-messages">RedmineLink</a>
 * notation in changelog messages.
 * 
 * @author gaooh
 * @date 2008/10/13
 */
public class RedmineLinkAnnotator extends ChangeLogAnnotator {

	@Override
	public void annotate(AbstractBuild<?, ?> build, Entry change, MarkupText text) {
		RedmineProjectProperty rpp = build.getProject().getProperty(RedmineProjectProperty.class);
        if(rpp == null || rpp.redmineWebsite == null) { // not configured
            return; 
        }

        String url = rpp.redmineWebsite;
        for (LinkMarkup markup : MARKUPS) {
            markup.process(text, url);
		}
	}

	static final class LinkMarkup {
        private final Pattern pattern;
        private final String href;
        
        LinkMarkup(String pattern, String href) {
            pattern = NUM_PATTERN.matcher(pattern).replaceAll("([\\\\d|,| |&]+)"); // \\\\d becomes \\d when in the expanded text.
            pattern = ANYWORD_PATTERN.matcher(pattern).replaceAll("((?:\\\\w|[._-])+)");
            this.pattern = Pattern.compile(pattern);
            this.href = href;
        }

        void process(MarkupText text, String url) {
        	for(SubText st : text.findTokens(pattern)) {
        		String[] message = st.getText().split(" ", 2);
        		if (message.length > 1) {
        			String[] nums = message[1].split(",|&");
        			
        			if(nums.length > 1) {
        				int startpos = 0;
        				int endpos = message[0].length() + nums[0].length() + 1;
        				st.addMarkup(startpos, endpos, "<a href='"+url+ "issues/show/"+nums[0]+"'>", "</a>");
    				
        				startpos = endpos + 1;
        				endpos = startpos;
        			
        				for(int i = 1 ; i < nums.length ; i++) {
        					endpos += nums[i].length() ;
        					if(i != 1) {
        						endpos += 1;
        					}
        					if(endpos >= st.getText().length()) {
        						endpos = st.getText().length();
        					}
        					st.addMarkup(startpos, endpos, "<a href='"+url+"issues/show/"+nums[i].trim()+"'>", "</a>");
        					startpos = endpos + 1;
        					
        				}
        			} else {
        				st.surroundWith("<a href='"+url+href+"'>","</a>");
        			}
        		} else {
        			st.surroundWith("<a href='"+url+href+"'>","</a>");
        		}
    		}
        }

        private static final Pattern NUM_PATTERN = Pattern.compile("NUM");
        private static final Pattern ANYWORD_PATTERN = Pattern.compile("ANYWORD");
    }

    static final LinkMarkup[] MARKUPS = new LinkMarkup[] {
    	new LinkMarkup(
            "(?:#|refs |references |IssueID |fixes |closes )NUM",
            "issues/show/$1"),
        new LinkMarkup(
            "((?:[A-Z][a-z]+){2,})|wiki:ANYWORD",
            "wiki/$1$2"),
    };
}
