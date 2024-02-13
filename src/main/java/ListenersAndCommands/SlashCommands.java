package ListenersAndCommands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;

public class SlashCommands {
    private final JDA bot;
    private final OptionData category = new OptionData(OptionType.STRING ,"category", "This is the name of " +
            "the category.",true);
    protected SlashCommandData bulkChannelCreation;

    protected SlashCommandData bulkThreadCreation;

    protected int bulkChannelAmount =1;
    protected int amountOfThreads =1;
    //protected Guild guild;

    public SlashCommands (JDA bot){
        this.bot = bot;

        bulkChannelCreator(1);
        bulkThreadCreator(1);
        channelCreation();
    }
//    public SlashCommands (Guild guild){
//       this.guild = guild;
//        bulkChannelCreateor(1);
//        channelCreation();
//    }
//    public SlashCommands (JDA bot, int bulkChannelAmount){
//        //this.bot = bot;
//        bulkChannelCreateor(bulkChannelAmount); //creates channels for the given amount, default is 1
//        channelCreation();
//    }

    public void channelCreation(){
        //TODO category deletion
        //TODO bulk voice channel creation
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount of channels " +
                "you aim to create using /bulk_create", true);

        CommandData bulk = Commands.slash("bulk","Prerequisite for bulk channel creation " +
                        "if the number is >1")
                .addOptions(amount);

        OptionData[] navigationOptions = new OptionData[]{
                new OptionData(OptionType.STRING, "navcat", "Name of the category in which" +
                        " the navigation channel exists/ will be created", true),
                new OptionData(OptionType.STRING, "navchn", "Name of the channel in " +
                        "which the navigation embed will be sent.", true)
        };

        CommandData navigation =//send navigation embed
                Commands.slash("navigation", "Sends a navigation message containing the channel" +
                                "links in a selected channel.")
                        .addSubcommands(new SubcommandData("cattonav", "Sends an" +
                                "embed containing all channels and their links in this category")
                                .addOptions(navigationOptions)
                                .addOption(OptionType.CHANNEL, "copycategory", "An embed " +
                                                "containing all of the channel links of this category will be created."
                                        ,true))
                        .addSubcommands(new SubcommandData("chantonav", "Creates an embed conta" +
                                "ining the link of this channel.")
                                .addOptions(navigationOptions)
                                .addOption(OptionType.CHANNEL, "channel", "Name of the " +
                                        "Channel.", true));

        CommandData threadBulk =
                Commands.slash("numofthreads", "Prerequisite for bulk thread creation.")
                                .addOptions(amount);

        CommandData embedSender =
                Commands.slash("embed", "Sends an embed to the channel.")
                                .addOption(OptionType.STRING, "title", "Title of the " +
                                        "embed.", true)
                                        .addOption(OptionType.STRING, "desc", "Description of " +
                                                "the embed.", true)
                        .addOption(OptionType.STRING, "image", "Includes an image in the embed if the" +
                                "input is a URL.");
        CommandData log =
                Commands.slash("log", "Logs a given channel.")
                        .addOption(OptionType.INTEGER,"placeholder", "placeholder");

        bot.updateCommands().addCommands(bulk, navigation, threadBulk, embedSender
                , bulkThreadCreation, bulkChannelCreation, log).complete();
    }

    protected void bulkChannelCreator(int amountOfChannels){
        if(amountOfChannels<1){
            return;
        }
        bulkChannelCreation = Commands.slash("bulkcreate", "Bulk creates channels, default is 1.")
                .addOption(OptionType.STRING ,"category", "This is the name of " +
                        "the category.",true);

        //recreate bulkChannelCreation
        for(int i =0; i< amountOfChannels; i++){
            bulkChannelCreation
                    .addOption(OptionType.STRING, "channel"+i+"", "Name of channel"+i+".", true);
        }
        bulkChannelAmount = amountOfChannels;
        bot.upsertCommand(bulkChannelCreation).complete();
    }
    protected void bulkThreadCreator(int amountOfThreads){
        if(amountOfThreads<1){
            return;
        }
        bulkThreadCreation = Commands.slash("bulkthread", "Bulk creates threads.")
                .addOption(OptionType.CHANNEL, "threadchannel", "The channel in which the threads will be " +
                        "created.", true);
        //recreate bulkthread
        for(int i =0; i< amountOfThreads; i++){
            bulkThreadCreation
                    .addOption(OptionType.STRING, "thread"+i+"", "Name of thread"+i+".", true);
        }
        this.amountOfThreads = amountOfThreads;
        bot.upsertCommand(bulkThreadCreation).complete();
    }




    public int getBulkChannelAmount() {
        return bulkChannelAmount;
    }

    public int getAmountOfThreads() {
        return amountOfThreads;
    }

    public void setAmountOfThreads(int amountOfThreads) {
        this.amountOfThreads = amountOfThreads;
    }
}
