package ListenersAndCommands;

import HTMLBuilderAndMessageParser.Div;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
//        bulkChannel(event);
//        bulkThread(event);
        embeds(event);
        log(event);
        pdfActions(event);
        logCategory(event);
    }
    public void log(SlashCommandInteractionEvent event){
        //TODO make message amount adjustable with an amount command and apply its content to retrievePast
        //TODO categorize content (change structure to first create an array of messages and look for specific words
        // them to categorize them)
        //TODO display Category -> Channel -> Thread names above the title. If the importances of the divs are the same
        // they should be ordered according to their channel codes. A static hashmap can be used to store the divs
        // that are in the same channel as values in a key representing the channel and a super div could be created.
        if(!event.getName().equals("log")) return;
        event.deferReply(true).queue();
        List<Message> messages = event.getGuildChannel().getHistory().retrievePast(100).complete();
        messages = messages.stream().filter(message -> !message.getType().isSystem())
                .filter(message -> !message.getContentRaw().startsWith("/")).collect(Collectors.toList());
        event.getHook().sendFiles(htmlCreator(event, messages)).queue();
    }
    public void logCategory(SlashCommandInteractionEvent event){
        if(!event.getName().equals("logfrom")) return;
        event.deferReply(true).queue();
        //!event.getOption("logcat").getAsChannel().equals(Category.class)
        if(event.getOption("logcat").getAsChannel().getType() != ChannelType.CATEGORY){
            if(event.getOption("logcat").getAsChannel().getType().isAudio()){
                event.getHook().sendMessage("This isn't a Category or a Text Channel!")
                        .setEphemeral(true).queue();
                return;
            }
            List<Message> messages;
            if ( event.getOption("logcat").getAsChannel().getType().isThread()){
                ThreadChannel tc = event.getOption("logcat").getAsChannel().asThreadChannel();
                messages = tc.getHistory().retrievePast(100).complete();
            }else{
                TextChannel textChannel = event.getOption("logcat").getAsChannel().asTextChannel();
                messages = textChannel.getHistory().retrievePast(100).complete();
            }
            if(messages.isEmpty()){
                event.getHook().sendMessage("The Channel has no messages!").setEphemeral(true).queue();
                return;
            }
            messages = (ArrayList<Message>) messages.stream().filter(message -> !message.getType().isSystem())
                    .filter(message -> !message.getContentRaw().startsWith("/")).collect(Collectors.toList());
            event.getHook().sendFiles(htmlCreator(event, messages)).queue();
            return;
        }
        Category category =event.getGuild().getCategoryById(event.getOption("logcat").getAsChannel().getId());
        List<TextChannel> textChannels = category.getTextChannels();
        if(textChannels.isEmpty()) {
            event.getHook().sendMessage("No Channels exist in the named Category!")
                    .setEphemeral(true).queue();
            return;
        }
        ArrayList<Message> messages = new ArrayList<>();
        for(TextChannel t : textChannels){
            messages.addAll(t.getHistory().retrievePast(100).complete());
            List<ThreadChannel> threads =t.getThreadChannels();
            if(!threads.isEmpty()) {
                for (ThreadChannel trc : threads) {
                    messages.addAll(trc.getHistory().retrievePast(100).complete());
                }
            }
        }
        messages = (ArrayList<Message>) messages.stream().filter(message -> !message.getType().isSystem())
                .filter(message -> !message.getContentRaw().startsWith("/")).collect(Collectors.toList());
        event.getHook().sendFiles(htmlCreator(event, messages)).queue();
    }



    public void embeds(SlashCommandInteractionEvent event){
        if(!event.getName().equals("embed")) return;
        event.deferReply().queue();
        MessageEmbed embed =titleDescEmbed(event);
        EmbedBuilder eb = new EmbedBuilder(embed);
        if(event.getOption("image")!=null){
            eb.setImage(event.getOption("image").getAsAttachment().getUrl());
        }
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
//    public void bulkThread(SlashCommandInteractionEvent event){//this and bulkchannel could be merged with a generic method
//        if(!event.getName().equals("numofthreads") && !event.getName().equals("bulkthread")) return;
//        event.deferReply().queue();
//        if(event.getName().equals("numofthreads")){
//            threadNumberDefined = true;
//            int bulkAmount = event.getOption("amount").getAsInt();
//            bulk.setAmountOfThreads(bulkAmount);
//
//            if(bulkAmount > 15){
//                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount was too much!**")
//                        .setDescription("bulkhead is now available with 15 channel options.")
//                        .setColor(Color.orange).build()).queue();
//                bulk.setAmountOfThreads(15);
//            }else{
//                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount has been set**")
//                        .setDescription("bulkhead is now available")
//                        .setColor(Color.green).build()).queue();
//            }
//            bulkThreadAmount = bulk.getAmountOfThreads();
//            bulk.bulkThreadCreator(bulkThreadAmount);
//            event.getJDA().upsertCommand(bulk.bulkThreadCreation).complete();
//        }
//        if(!threadNumberDefined){
//            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
//                    .setDescription("You have to set the amount of threads you want to create with /numofthreads" +
//                            " first!")
//                    .setColor(Color.red).build()).queue();
//        }
//
//        if(event.getName().equals("bulkthread")) {
//            Guild guild = event.getGuild();
//            boolean threadExists = true;
//            String channelForThreadsId = event.getOption("threadchannel").getAsChannel().getId();
//
//            //TODO standardize checks with methods
//            boolean errorOccured = false;
//            if (guild.getCategories().isEmpty()) {
//                errorOccured = true;
//            }
//            List<TextChannel> textChannelList = guild.getTextChannels();
//            if (textChannelList.stream().noneMatch(textChannel -> textChannel.getId().equals(channelForThreadsId))) {
//                errorOccured = true;
//            }
//            if(errorOccured ) {
//                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
//                        .setDescription("Channel for thread creation does not exist!" +
//                                " first!")
//                        .setColor(Color.red).build()).queue();
//                return;
//            }
//            TextChannel channelForThread = textChannelList.stream()
//                    .filter(textChannel -> textChannel.getId().equals(channelForThreadsId))
//                    .toList().getFirst();
//
//            String[] threadNames = new String[bulkThreadAmount];
//            for(int i =0; i < bulkThreadAmount; i ++){
//                threadNames[i] = event.getOption("thread" + i + "").getAsString();
//            }
//            EmbedBuilder eb = new EmbedBuilder();
//            String embedDescription ="";
//            int counter =0;
//            List<ThreadChannel> threadChannelList;
//            for(String threadNamez: threadNames){
//                threadChannelList = channelForThread.getThreadChannels();
//                if(threadChannelList.stream()
//                        .noneMatch(threadChannel1 -> threadChannel1.getName().equals(threadNamez))){
//                    threadExists =false;
//                    channelForThread.createThreadChannel(threadNamez).complete();
//                }
//                if(threadExists){
//                    embedDescription +="**"+threadNamez+" has not been created**\n" +
//                            "Since the thread already exists, a new channel has not been created.\n";
//                    eb.setColor(Color.orange);
//                    counter++;
//                }
//                threadExists = true;
//            }
//            if(count == bulkThreadAmount) eb.setTitle("**FATAL ERROR").setColor(Color.red);
//            else if (count==0) eb.setTitle("**Task completed without any errors**").setColor(Color.green);
//            embedDescription+= "Task complete.";
//            eb.setDescription(embedDescription);
//            event.getHook().sendMessageEmbeds(eb.build()).queue();
//        }
//
//    }
//    public void bulkChannel(SlashCommandInteractionEvent event){
//        if(!event.getName().equals("bulk") && !event.getName().equals("bulkcreate")) return;
//        event.deferReply().queue();
//        if(event.getName().equals("bulk")){
//            bulkHasBeenSet = true;
//            int bulkAmount = event.getOption("amount").getAsInt();
//            if(bulkAmount > 15){
//                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount was too much!**")
//                        .setDescription("/bulkcreate is now available with 15 channel options.")
//                        .setColor(Color.orange).build()).queue();
//                bulkAmount = 15;
//            }else{
//            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount has been set**")
//                    .setDescription("/bulkcreate is now available")
//                    .setColor(Color.green).build()).queue();
//            }
//            bulk.bulkChannelCreator(bulkAmount);
//            bulkChannelAmount = bulk.getBulkChannelAmount();
//            event.getJDA().upsertCommand(bulk.bulkChannelCreation).complete();
//        }
//        if(!bulkHasBeenSet){
//            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**ERROR**")
//                    .setDescription("You have to set the amount of channels you want to create with /bulk first!")
//                    .setColor(Color.red).build()).queue();
//        }
//
//        if(event.getName().equals("bulkcreate")) {
//            Guild guild = event.getGuild();
//            boolean channelExists = true;
//            String catName = event.getOption("category").getAsString();
//            if (guild.getCategories().isEmpty()) {
//                guild.createCategory(catName).complete();
//            }
//            List<Category> categoriesList = guild.getCategories(); //there has to be another way of doing this
//            if (categoriesList.stream().noneMatch(category -> category.getName().equals(catName))) {
//                guild.createCategory(catName).complete();
//            }
//            Category chnCat = guild.getCategoriesByName(catName, false).getFirst();
//            String[] channelNames = new String[bulkChannelAmount];
//            for(int i =0; i < bulkChannelAmount; i ++){
//                channelNames[i] = event.getOption("channel"+i).getAsString().toLowerCase();
//            }
//            EmbedBuilder eb = new EmbedBuilder();
//            String embedDescription ="";
//            int count =0;
//            List<TextChannel> textChannelList;
//            for(String channelName: channelNames){
//                textChannelList = chnCat.getTextChannels();
//                if(textChannelList.stream().noneMatch(textChannel -> textChannel.getName().equals(channelName))){
//                    channelExists =false;
//                    chnCat.createTextChannel(channelName).complete();
//                }
//                if(channelExists){
//                    embedDescription +="**"+channelName+" has not been created**\n" +
//                                    "Since the channel already exists, a new channel has not been created.\n";
//                    eb.setColor(Color.orange);
//                    count++;
//                }
//                channelExists = true;
//            }
//            if(count == bulkChannelAmount) eb.setTitle("**FATAL ERROR").setColor(Color.red);
//            else if (count==0) eb.setTitle("**Task completed without any errors**").setColor(Color.green);
//            embedDescription+= "Task complete.";
//            eb.setDescription(embedDescription);
//            event.getHook().sendMessageEmbeds(eb.build()).queue();
//        }
//    }
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

    public void pdfActions(SlashCommandInteractionEvent event){
        if(!event.getName().equals("embedpdf")) return;
        event.deferReply(true).queue();
        //Message.Attachment path = event.getOption("file").getAsAttachment();
        int pageNum = event.getOption("page").getAsInt()-1;
        File pdf = new File(System.getenv("PDF_PATH"));

        try {
            PDDocument document = PDDocument.load(pdf);
            if(pageNum > document.getNumberOfPages()){
                event.getHook().sendMessage("Error! Page number is not found in the given document!").queue();
                return;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bI =renderer.renderImage(pageNum);
            String filePath = "PDFScreenShots";
            Path path = Path.of(filePath);
            if(!Files.exists(path)) Files.createDirectory(path);
            filePath+= File.pathSeparatorChar+pageNum+"_Screenshot.png";
            File outputPage = new File(filePath);
            ImageIO.write(bI, "png",outputPage);
            document.close();
            File image = new File(filePath);
            FileUpload fileUpload = FileUpload.fromData(image);
            Files.delete(Path.of(filePath));
            Message m = event.getHook().setEphemeral(true).sendFiles(fileUpload).complete();
            String fileURL =m.getAttachments().getFirst().getUrl();
            event.getHook().deleteMessageById(m.getId()).queue();
            EmbedBuilder eb = new EmbedBuilder(titleDescEmbed(event));
            eb.setImage(fileURL);
            event.getHook().setEphemeral(false).sendMessageEmbeds(eb.build()).queue();
        } catch (IOException e) {
            event.getHook().setEphemeral(true).sendMessage("ERROR!").queue();
            throw new RuntimeException("PDF file could not be loaded. pdfActions in EventListener");
        }
    }
    public MessageEmbed titleDescEmbed(SlashCommandInteractionEvent event){
        String title = event.getOption("title").getAsString();
        title = "**"+title+"**";
        String desc = event.getOption("desc").getAsString();
        EmbedBuilder eb = new EmbedBuilder();
        Color c = Color.getHSBColor((float)Math.random(),(float)Math.random(),(float)Math.random());
        return eb.setTitle(title).setDescription(desc).setColor(c).build();
    }

    public FileUpload htmlCreator(SlashCommandInteractionEvent event, List<Message> messages){
        boolean eventComplete = false;
        BufferedWriter writer = null;
        String embedLikeText="";
        FileUpload fu = null;
        File file = new File("MessageFiles"+File.separatorChar+event.getChannel().getName()+".html");
        try { //Known issue: Links containing ? may cause an error by <a href
            writer = new BufferedWriter(new FileWriter(file));
            embedLikeText += "<!DOCTYPE html><html>" +
                    "<head>" +
                    "    <title>"+event.getChannel().getName().toUpperCase()+"</title>" +
                    "    <style>" +
                    "          body {" +
                    "            background-color: #121212;" +
                    "            color: #e0e0e0; /" +
                    "            display: flex;" +
                    "            flex-direction: column;" +
                    "            align-items: center; /* Center the rectangles */" +
                    "            padding: 20px;" +
                    "        }" +
                    "        .container {" +
                    "            background-color: #BB86FC;" +
                    "            border-radius: 20px;" +
                    "            margin: 10px 0;" +
                    "            color: #e0e0e0; " +
                    "            font-family: Verdana, Geneva, Tahoma, sans-serif " +
                    "            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); " +
                    "            position: relative; " +
                    "            padding-top: 40px; " +
                    "            padding-bottom: 40px;" +
                    "        }" +
                    "        .toggle-button {" +
                    "            position: absolute;" +
                    "            top: 17.5px;" +
                    "            left: 10px;" +
                    "            cursor: pointer;" +
                    "            padding: 40px 40px;" +
                    "            background-color: #121212; " +
                    "            color: #FFFAFF; " +
                    "            border: none;" +
                    "            border-radius: 10px; " +
                    "        }" +
                    "        .content {" +
                    "            display: none; " +
                    "            padding: 15px;" +
                    "            text-align: center;" +
                    "        }" +
                    "        h2 {" +
                    "            text-align: center;" +
                    "            cursor: pointer;" +
                    "            margin: 10px 0;" +
                    "            color: #121212;" +
                    "width: 100%;" +
                    "        }" +
                    "        p{" +
                    "            font-size: 20px;" +
                    "            color: #121212;"+
                    "        }" +
                    "        img {" +
                    "            max-width: 100%; " +
                    "            height: auto; " +
                    "            border-radius: 10px; " +
                    "            display: block; " +
                    "            margin: 0 auto;" +
                    "        }" +
                    "    </style>" +
                    "</head>" +
                    "<body>" +
                    "<h1>"+event.getChannel().getName().toUpperCase()+"</h1>";
            ArrayList<Div> divs = new ArrayList<>();
            for (Message message : messages) {
                divs.add(new Div(message));
            }
            divs.sort((o1, o2) -> o1.compare(o1,o2));
            for(Div div: divs){
                embedLikeText+= div.toString();
            }
            embedLikeText+="<script>" +
                    "    function toggleContent(container) {" +
                    "        var content = container.querySelector(\".content\");" +
                    "        content.style.display = content.style.display === \"block\" ? \"none\" : \"block\";" +
                    "    }" +
                    "</script></body> </html>";


            writer.write(embedLikeText);
            fu = FileUpload.fromData(file);
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
        if(!eventComplete) {
            event.getHook().sendMessage("Failiure.").queue();
            return fu;
        }
        return fu;
    }
}
