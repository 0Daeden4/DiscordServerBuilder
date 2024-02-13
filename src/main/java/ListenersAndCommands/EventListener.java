package ListenersAndCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class EventListener extends ListenerAdapter {
    private boolean bulkHasBeenSet= false;
    private int bulkChannelAmount = 1;
    private boolean threadNumberDefined = false;
    private int bulkThreadAmount = 1;
    private byte count =0;
    private SlashCommands bulk;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(count == 0){
            count++;
            bulk =new SlashCommands(event.getJDA());
        }
        channelNavigator(event);
        bulkChannel(event);
        bulkThread(event);
        embeds(event);
        log(event);
    }
    public void log(SlashCommandInteractionEvent event){
        //TODO add thread crawl
        //TODO make message amount adjustable with an amount command and apply its content to retrievePast
        if(!event.getName().equals("log")) return;
        boolean eventComplete = false;
        event.deferReply().queue();
         List<Message> messages = event.getChannel().getHistory().retrievePast(100).complete();
        BufferedWriter writer = null;
        String embedLikeText="";
        File file = new File("MessageFiles"+File.separatorChar+"messageLogs .html");
        try {
            writer = new BufferedWriter(new FileWriter(file));
            embedLikeText += "<!DOCTYPE html><html><body>" +
                    "<h1>LOGS</h1>";
            for (Message message : messages) {
                if(message.getContentDisplay().startsWith("/")) continue;
                embedLikeText += "<div style=\"border: 2px solid black; padding: 10px; display: grid; grid-template-columns: auto auto;\">"
                        +"<small><p>**Author:** \n" + message.getAuthor().getName().toUpperCase() + "\n</p></small>" ;
                if(!message.getEmbeds().isEmpty()){
                    embedLikeText+="<p> " + message.getEmbeds().getFirst().getTitle()+"\n"+
                            "" +message.getEmbeds().getFirst().getDescription()+"\n</p>";
                    if(message.getEmbeds().getFirst().getImage() !=null){
                        embedLikeText+=   "<img src=\""+message.getEmbeds().getFirst()
                                .getImage().getUrl() +
                            "\" alt=\"image\" style=\"width:40%; height:auto; overflow: auto;\"> " ;
                    }
                    if(message.getEmbeds().getFirst().getDescription().contains("http") ){
                        String siteLink =extractLink(message.getEmbeds().getFirst().getDescription());
                        embedLikeText =embedLikeText.replace(siteLink, "<a href=\""+siteLink+"\"> **Link**</a>");
                        embedLikeText += "<iframe src=\""+siteLink+"\" width=\"600\" height=\"400\">" +
                                "</iframe>";
                    }
                }else{
                    embedLikeText +="<p>**Content:** \n" + message.getContentDisplay() + "\n </p>";
                    if(message.getContentDisplay().contains("http") ){
                        String siteLink =extractLink(message.getContentDisplay());
                        embedLikeText =embedLikeText.replace(siteLink, "<a href=\""+siteLink+"\"> **Link**</a>");
                        embedLikeText += "<iframe src=\""+siteLink+"\" width=\"600\" height=\"400\">" +
                                "</iframe>";

                    }
                    if( !message.getAttachments().isEmpty()){
                        String attachmentURL =message.getAttachments().getFirst().getUrl();
                        embedLikeText+=   "<img src=\""+attachmentURL +
                                "\" alt=\"image\" style=\"width:40%; height:auto; overflow: auto;\"> " ;
                    }

                }

                embedLikeText+="</div>";
                        //"Timestamp: " + message.getTimeCreated().
            }
            embedLikeText+="</body> </html>";
            embedLikeText =embedLikeText.replace("**", "<b>");
            embedLikeText = embedLikeText.replace("\n", "<br>");

            embedLikeText = replaceEverySecondOccurrence(embedLikeText, "<b>", "</b>");
            writer.write(embedLikeText);
            event.getHook().sendFiles(FileUpload.fromData(file)).queue();
            eventComplete =true;
        } catch (IOException e) {
            e.printStackTrace();
            event.getHook().sendMessage("error").queue();
        }finally {
            try {
                writer.close();
                file.delete();
            } catch (IOException e) {
                throw new RuntimeException("Writer could not be closed.");
            }
        }
        if(!eventComplete) event.getHook().sendMessage("Failiure.").queue();
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
    private String extractLink(String text){
        String urlPattern = "(http://www\\.|https://www\\.|http://|https://)[a-zA-Z0-9\\-\\.]+\\." +
                "[a-zA-Z]{2,5}(:[0-9]{1,5})?(/\\S*)?";
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "Invalid Url.";
        }

    }
    public void embeds(SlashCommandInteractionEvent event){
        if(!event.getName().equals("embed")) return;
        event.deferReply().queue();
        String title = event.getOption("title").getAsString();
        title = "**"+title+"**";
        String desc = event.getOption("desc").getAsString();
        EmbedBuilder eb = new EmbedBuilder();
        Color c = Color.getHSBColor((float)Math.random(),(float)Math.random(),(float)Math.random());
        if(event.getOption("image")!=null){
            eb.setImage(event.getOption("image").getAsAttachment().getUrl());
        }
        eb.setTitle(title).setDescription(desc).setColor(c);
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
    public void bulkThread(SlashCommandInteractionEvent event){//this and bulkchannel could be merged with a generic method
        if(!event.getName().equals("numofthreads") && !event.getName().equals("bulkthread")) return;
        event.deferReply().queue();
        if(event.getName().equals("numofthreads")){
            threadNumberDefined = true;
            int bulkAmount = event.getOption("amount").getAsInt();
            bulk.setAmountOfThreads(bulkAmount);

            if(bulkAmount > 15){
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount was too much!**")
                        .setDescription("bulkhead is now available with 15 channel options.")
                        .setColor(Color.orange).build()).queue();
                bulk.setAmountOfThreads(15);
            }else{
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount has been set**")
                        .setDescription("bulkhead is now available")
                        .setColor(Color.green).build()).queue();
            }
            bulkThreadAmount = bulk.getAmountOfThreads();
            bulk.bulkThreadCreator(bulkThreadAmount);
            event.getJDA().upsertCommand(bulk.bulkThreadCreation).complete();
        }
        if(!threadNumberDefined){
            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
                    .setDescription("You have to set the amount of threads you want to create with /numofthreads" +
                            " first!")
                    .setColor(Color.red).build()).queue();
        }

        if(event.getName().equals("bulkthread")) {
            Guild guild = event.getGuild();
            boolean threadExists = true;
            String channelForThreadsId = event.getOption("threadchannel").getAsChannel().getId();

            //TODO standardize checks with methods
            boolean errorOccured = false;
            if (guild.getCategories().isEmpty()) {
                errorOccured = true;
            }
            List<TextChannel> textChannelList = guild.getTextChannels();
            if (textChannelList.stream().noneMatch(textChannel -> textChannel.getId().equals(channelForThreadsId))) {
                errorOccured = true;
            }
            if(errorOccured ) {
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
                        .setDescription("Channel for thread creation does not exist!" +
                                " first!")
                        .setColor(Color.red).build()).queue();
                return;
            }
            TextChannel channelForThread = textChannelList.stream()
                    .filter(textChannel -> textChannel.getId().equals(channelForThreadsId))
                    .toList().getFirst();

            String[] threadNames = new String[bulkThreadAmount];
            for(int i =0; i < bulkThreadAmount; i ++){
                threadNames[i] = event.getOption("thread" + i + "").getAsString();
            }
            EmbedBuilder eb = new EmbedBuilder();
            String embedDescription ="";
            int counter =0;
            List<ThreadChannel> threadChannelList;
            for(String threadNamez: threadNames){
                threadChannelList = channelForThread.getThreadChannels();
                if(threadChannelList.stream()
                        .noneMatch(threadChannel1 -> threadChannel1.getName().equals(threadNamez))){
                    threadExists =false;
                    channelForThread.createThreadChannel(threadNamez).complete();
                }
                if(threadExists){
                    embedDescription +="**"+threadNamez+" has not been created**\n" +
                            "Since the thread already exists, a new channel has not been created.\n";
                    eb.setColor(Color.orange);
                    counter++;
                }
                threadExists = true;
            }
            if(count == bulkThreadAmount) eb.setTitle("**FATAL ERROR").setColor(Color.red);
            else if (count==0) eb.setTitle("**Task completed without any errors**").setColor(Color.green);
            embedDescription+= "Task complete.";
            eb.setDescription(embedDescription);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        }

    }
    public void bulkChannel(SlashCommandInteractionEvent event){
        if(!event.getName().equals("bulk") && !event.getName().equals("bulkcreate")) return;
        event.deferReply().queue();
        if(event.getName().equals("bulk")){
            bulkHasBeenSet = true;
            int bulkAmount = event.getOption("amount").getAsInt();
            if(bulkAmount > 15){
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount was too much!**")
                        .setDescription("/bulkcreate is now available with 15 channel options.")
                        .setColor(Color.orange).build()).queue();
                bulkAmount = 15;
            }else{
            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount has been set**")
                    .setDescription("/bulkcreate is now available")
                    .setColor(Color.green).build()).queue();
            }
            bulk.bulkChannelCreator(bulkAmount);
            bulkChannelAmount = bulk.getBulkChannelAmount();
            event.getJDA().upsertCommand(bulk.bulkChannelCreation).complete();
        }
        if(!bulkHasBeenSet){
            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
                    .setDescription("You have to set the amount of channels you want to create with /bulk first!")
                    .setColor(Color.red).build()).queue();
        }

        if(event.getName().equals("bulkcreate")) {
            Guild guild = event.getGuild();
            boolean channelExists = true;
            String catName = event.getOption("category").getAsString();
            if (guild.getCategories().isEmpty()) {
                guild.createCategory(catName).complete();
            }
            List<Category> categoriesList = guild.getCategories(); //there has to be another way of doing this
            if (categoriesList.stream().noneMatch(category -> category.getName().equals(catName))) {
                guild.createCategory(catName).complete();
            }
            Category chnCat = guild.getCategoriesByName(catName, false).getFirst();
            String[] channelNames = new String[bulkChannelAmount];
            for(int i =0; i < bulkChannelAmount; i ++){
                channelNames[i] = event.getOption("channel"+i).getAsString().toLowerCase();
            }
            EmbedBuilder eb = new EmbedBuilder();
            String embedDescription ="";
            int count =0;
            List<TextChannel> textChannelList;
            for(String channelName: channelNames){
                textChannelList = chnCat.getTextChannels();
                if(textChannelList.stream().noneMatch(textChannel -> textChannel.getName().equals(channelName))){
                    channelExists =false;
                    chnCat.createTextChannel(channelName).complete();
                }
                if(channelExists){
                    embedDescription +="**"+channelName+" has not been created**\n" +
                                    "Since the channel already exists, a new channel has not been created.\n";
                    eb.setColor(Color.orange);
                    count++;
                }
                channelExists = true;
            }
            if(count == bulkChannelAmount) eb.setTitle("**FATAL ERROR").setColor(Color.red);
            else if (count==0) eb.setTitle("**Task completed without any errors**").setColor(Color.green);
            embedDescription+= "Task complete.";
            eb.setDescription(embedDescription);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        }
    }
    public void channelNavigator(SlashCommandInteractionEvent event){
        //navigation Channel creator
        if(!event.getName().equals("navigation")) return;
        event.deferReply().queue();
        Guild guild = event.getGuild();
        List<Category> categoriesList = guild.getCategories();
        String navCatName= event.getOption("navcat").getAsString();
        String navChnName = event.getOption("navchn").getAsString().toLowerCase();
        if(categoriesList.stream().noneMatch(category -> category.getName().equals(navCatName))){
            guild.createCategory(navCatName).complete();
        }
        Category navCat = guild.getCategoriesByName(navCatName,false).getFirst();
        List<GuildChannel>textChannelList;
        textChannelList = navCat.getChannels();
        if(textChannelList.stream().noneMatch(textChannel -> textChannel.getName().equals(navChnName))){
            navCat.createTextChannel(navChnName).complete();
        }
        TextChannel navChn = guild.getTextChannelsByName(navChnName, false).getFirst();
        String guildID = guild.getId();
        String channelID ="";
        String channelURL ="";
        EmbedBuilder eb = new EmbedBuilder();

        if(event.getSubcommandName().equals("cattonav")){
            ChannelType channelType = event.getOption("copycategory").getChannelType();
            String catToCopyName= event.getOption("copycategory").getAsChannel().getName();
            if(channelType != ChannelType.CATEGORY){
                event.getHook().sendMessageEmbeds(eb.setTitle("**"+catToCopyName+" IS NOT A " +
                        "CATEGORY!**").setColor(Color.red).build()).queue();
                return;
            }
            Category toCopy = guild.getCategoriesByName(catToCopyName, false).getFirst();
            if(toCopy.getTextChannels().isEmpty()) return;
            textChannelList = toCopy.getChannels();
            List<String> urls = textChannelList.stream()
                    .map(channel-> "**"+channel.getName()+" :** https://discord.com/channels/"
                            +guildID+"/"+channel.getId()+"\n\n").collect(Collectors.toList());
            String finalURL= "";
            for(String s: urls){
                finalURL+= s;
            }
            //String finalUrl = urls.toString().replaceAll("\\]", "")
              //      .replaceAll("\\[", "").replaceAll(",","");
            navChn.sendMessageEmbeds(eb.setDescription(finalURL).setColor(Color.green)
                    .setTitle("**"+catToCopyName +" Navigation**").build()).queue();
        }
        else if(event.getSubcommandName().equals("chantonav")){
            channelID =event.getOption("channel").getAsChannel().getId();
            String channelName = event.getOption("channel").getAsChannel().getName();
            channelURL = "https://discord.com/channels/"+guildID+"/"+channelID;

            navChn.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("**Channel Navigator**")
                    .setDescription("**" + channelName + " :** " + channelURL).build()).queue();
        }
        event.getHook().sendMessageEmbeds(new EmbedBuilder().setDescription("**Task Complete.**")
                .setColor(Color.GREEN).build()).queue();

    }
}
