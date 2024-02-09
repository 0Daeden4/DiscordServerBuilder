package ListenersAndCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;


public class EventListener extends ListenerAdapter {
    private boolean bulkHasBeenSet= false;
    private int bulkChannelAmount = 1;
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        channelNavigator(event);
        bulkChannel(event);
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
                SlashCommands bulk = new SlashCommands(event.getJDA(), bulkAmount);
                bulkChannelAmount = bulk.getBulkChannelAmount();
                return;
            }
            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("**Bulk amount has been set**")
                    .setDescription("/bulkcreate is now available")
                    .setColor(Color.green).build()).queue();
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
        categoriesList = guild.getCategories();
        Category navCat = guild.getCategoriesByName(navCatName,false).getFirst();
        List<TextChannel>textChannelList;
        textChannelList = navCat.getTextChannels();
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
            textChannelList = toCopy.getTextChannels();
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
            String channelName = event.getOption("channel").getAsChannel().getId();
            channelURL = "https://discord.com/channels/"+guildID+"/"+channelID;

            navChn.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("**Channel Navigator**")
                    .setDescription("**" + channelName + " :** " + channelURL).build()).queue();
        }
        event.getHook().sendMessageEmbeds(new EmbedBuilder().setDescription("**Task Complete.**")
                .setColor(Color.GREEN).build()).queue();

    }
}
