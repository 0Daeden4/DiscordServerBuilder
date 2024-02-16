package HTMLBuilderAndMessageParser;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Div implements Comparator {
    private String title;
    private String desc;
    private String img;
    private String link;
    private boolean isEmbed = false;
    private String div;
    private int reactionCount;
    private Message m;
    private Emoji question = Emoji.fromUnicode("U+2753");
    private Emoji extreme = Emoji.fromUnicode("U+203C");
    private Emoji ignore = Emoji.fromUnicode("U+274C");
    private Type type;

    @Override
    public int compare(Object o1, Object o2) {
        return -Integer.compare(((Div)o1).getImportance(),((Div)o2).getImportance());
    }

    public enum Type{
        QUESTION,
        IMPORTANT,
        EXTREME,
        IGNORE,
        NORMAL
    }
    public Div(Message m){
        this.m = m;
        String typeIndicator="";
        List<MessageReaction> mr = m.getReactions();
        reactionCount = mr.size();
        if(!mr.isEmpty()) {
            if (containsReaction(question)) {
                type = Type.QUESTION;
                typeIndicator += "{?}";
                reactionCount = 1000;
            } else if (containsReaction(ignore)) {
                type = Type.IGNORE;
                reactionCount = 0;
            } else if (reactionCount >= 1) {
                type = Type.IMPORTANT;
                typeIndicator += "!!";
            } else if (containsReaction(extreme)) {
                type = Type.EXTREME;
                typeIndicator += "@REALLY IMPORTANT!";
                reactionCount = 999;
            }
        }
        else {
            type = Type.NORMAL;
        }
        if(type == Type.IGNORE){
            div ="";
        }
        else {
            if(reactionCount>=1) typeIndicator += " Importance Level: "+reactionCount;
            if (!m.getEmbeds().isEmpty()) {
                MessageEmbed embed = m.getEmbeds().getFirst();
                isEmbed = true;
                title = setTitle(embed.getTitle() + " " + typeIndicator);
                desc = setDesc(embed.getDescription());
            } else {
                isEmbed = false;
                title = setTitle(m.getAuthor().getName() + " " + typeIndicator);
                desc = setDesc(m.getContentDisplay());
            }
            img = getImg();
            link = getLinks();
            String tempDesc;
            tempDesc = desc;
            setAHref();
            //desc = desc.replace("\n", "<br>");

            div = "<div class=\"container\">" +
                    "<button class=\"toggle-button\" onclick=\"toggleContent(this.parentElement)\"><b>o</b></button>"
                    + title + desc + img + "<br>" + extractIframe(tempDesc) + "</div></div>\r\n";
        }
    }
    private String replaceEverySecondOccurrence(String text, String search, String replacement) {
        int index = -1;
        boolean replace = false;
        StringBuilder sb = new StringBuilder(text);
        while ((index = sb.indexOf(search, index + 1)) != -1) {
            if (replace) {
                sb.replace(index, index + search.length(), replacement);
                index += replacement.length() - search.length();
            }
            replace = !replace;
        }

        return sb.toString();
    }
    private boolean containsReaction(Emoji emoji){
        String unicode = emoji.getAsReactionCode();
        for (MessageReaction reaction : m.getReactions()) {
            if(unicode.equals(reaction.getEmoji().getAsReactionCode())) return true;
        }
        return false;
    }
    public String toString(){
        return div;
    }
    public int getImportance(){
        return reactionCount;
    }
    public void setImportance(int importance){
        reactionCount = importance;
    }
    public boolean isOfType(Type type){
        return this.type == type;
    }

    public String getDiv() {
        return div;
    }

    private void setAHref(){
        if(!link.contentEquals("")){
            desc =desc.replace(link, "<a href=\""+link+"\"> **Link**</a>");
        }
    }
    private String setTitle(String s){
        s = replaceEverySecondOccurrence(s, "**", "</b>");
        s = s.replace("**", "<b>");
        s = replaceEverySecondOccurrence(s,"*","</i>" );
        s = s.replace("*", "</i>");
        return "<h2>"+s+"</h2>";
    }
    private String setDesc(String s){
        s = replaceEverySecondOccurrence(s, "**", "</b>");
        s = s.replace("**", "<b>");
        s = replaceEverySecondOccurrence(s,"*","</i>" );
        s = s.replace("*", "</i>");
        return "<div class=\"content\"><p>" +s+" <br></p>";
    }
    private boolean isEmbed(){
        return isEmbed;
    }
    private String getImg(){
        String imgLink ="";
        if(isEmbed()){
            MessageEmbed embed = m.getEmbeds().getFirst();
            if(embed.getImage() !=null){
                imgLink =embed.getImage().getUrl();
            }else{
                return "";
            }
        }else{
            if(!m.getAttachments().isEmpty()){
                imgLink =m.getAttachments().getFirst().getUrl();
            }else return "";
        }
        return "<img src=\""+ imgLink +
                "\" alt=\"image\" style=\"width:40%; height:auto; overflow: auto;\"> ";
    }
    private String getLinks(){
        if(desc.contains("http")){
            return extractLink(desc);
        }
        return "";
    }
    private String extractLink(String text){
        String urlPattern = "(http://www\\.|https://www\\.|http://|https://)[a-zA-Z0-9\\-\\.]+\\." +
                "[a-zA-Z]{2,5}(:[0-9]{1,5})?(/\\S*)?";
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "";
        }
    }
    private String extractIframe(String text){
        String siteLink =  extractLink(text);
        if(siteLink.contentEquals("")) return "";
        //turn youtube links to youtube embed links
        siteLink = "<iframe src=\""+siteLink +"\"";
        if(siteLink.contains("youtu.be/")|| siteLink.contains("youtube.com")) {
            if (siteLink.contains("watch?v=")) {
                siteLink = siteLink.replace("watch?v=", "");
            }
            siteLink = siteLink.replace("youtube.com/", "youtube.com/embed/");
            siteLink = siteLink.replace("youtu.be/", "youtube.com/embed/");

            siteLink+= "width=\"560\" height=\"315\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay" +
                    "; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen>";
        }else siteLink = siteLink +"width=\"600\" height=\"400\">";
        siteLink += "</iframe>";
        return siteLink;
    }


}
