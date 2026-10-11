import os
import logging

import discord
from discord import app_commands
from discord.ext import commands
from dotenv import load_dotenv


# ==========================================
# CONFIGURATION
# ==========================================

load_dotenv()

token = os.getenv("DISCORD_TOKEN")

if not token:
    raise RuntimeError("DISCORD_TOKEN is missing!")


# Logging
handler = logging.FileHandler(
    filename="discord.log",
    encoding="utf-8",
    mode="a"
)


# Discord Intents
intents = discord.Intents.default()
intents.members = True


# ==========================================
# BOT INITIALIZATION
# ==========================================

class PluginUpdateBot(commands.Bot):

    async def setup_hook(self):

        # Synchronise slash commands with Discord
        synced = await self.tree.sync()

        print(f"Successfully synced {len(synced)} slash commands!")


# Mention-only prefix for the underlying commands.Bot.
# All commands below use Discord's native / commands.
bot = PluginUpdateBot(
    command_prefix=commands.when_mentioned,
    intents=intents
)


# ==========================================
# BOT EVENTS
# ==========================================

@bot.event
async def on_ready():

    print(f"PluginUpdateWatch Bot is online!")
    print(f"Logged in as: {bot.user}")
    print(f"Connected to {len(bot.guilds)} servers")


@bot.event
async def on_member_join(member):

    try:
        await member.send(
            f"Welcome to the PluginUpdateWatch server, {member.name}!"
        )

    except discord.Forbidden:
        print(f"Could not DM {member.name}")


# ==========================================
# ANNOUNCEMENT COMMAND
# ==========================================

@bot.tree.command(
    name="announce",
    description="Send a formatted announcement in a channel"
)
@app_commands.default_permissions(manage_guild=True)
@app_commands.describe(
    channel="Channel for announcement",
    title="Announcement title",
    message="Announcement body"
)
async def announce(
    interaction: discord.Interaction,
    channel: discord.TextChannel,
    title: str,
    message: str
):

    # Check server permissions
    if (
        not interaction.guild
        or not isinstance(interaction.user, discord.Member)
        or not interaction.user.guild_permissions.manage_guild
    ):

        await interaction.response.send_message(
            "You need Manage Server permission.",
            ephemeral=True
        )
        return

    # Check channel belongs to server
    if channel.guild.id != interaction.guild.id:

        await interaction.response.send_message(
            "Select a channel in this server.",
            ephemeral=True
        )
        return

    # Validate message length
    if len(title) > 250 or len(message) > 4000:

        await interaction.response.send_message(
            "Title or message is too long.",
            ephemeral=True
        )
        return

    # Build announcement embed
    embed = discord.Embed(
        title=f"📢 {title}",
        description=message,
        color=0x5865F2
    )

    embed.set_footer(
        text=interaction.guild.name,
        icon_url=interaction.guild.icon.url if interaction.guild.icon else None
    )

    # Send announcement
    try:

        await channel.send(
            embed=embed,
            allowed_mentions=discord.AllowedMentions.none()
        )

    except discord.HTTPException:

        await interaction.response.send_message(
            "Cannot post there. Check my channel permissions.",
            ephemeral=True
        )
        return

    await interaction.response.send_message(
        f"Announcement posted in {channel.mention}.",
        ephemeral=True
    )


# ==========================================
# DIRECT MESSAGE COMMAND
# ==========================================

@bot.tree.command(
    name="dm",
    description="Send yourself a direct message"
)
@app_commands.describe(
    message="Message to send"
)
async def dm(
    interaction: discord.Interaction,
    message: str
):

    try:

        await interaction.user.send(
            f"You said: {message}"
        )

        await interaction.response.send_message(
            "Message sent to your DMs!",
            ephemeral=True
        )

    except discord.Forbidden:

        await interaction.response.send_message(
            "I couldn't DM you. Check your privacy settings.",
            ephemeral=True
        )


# ==========================================
# REPLY COMMAND
# ==========================================

@bot.tree.command(
    name="reply",
    description="Get a reply from the bot"
)
async def reply(interaction: discord.Interaction):

    await interaction.response.send_message(
        "This is a reply to your command!"
    )


# ==========================================
# POLL COMMAND
# ==========================================

@bot.tree.command(
    name="poll",
    description="Create a poll with reactions"
)
@app_commands.describe(
    question="The question for your poll"
)
async def poll(
    interaction: discord.Interaction,
    question: str
):

    if len(question) > 4000:

        await interaction.response.send_message(
            "Your poll question is too long.",
            ephemeral=True
        )
        return

    embed = discord.Embed(
        title="📊 New Poll",
        description=question,
        color=0x5865F2
    )

    embed.set_footer(
        text=f"Created by {interaction.user.display_name}"
    )

    await interaction.response.send_message(embed=embed)

    # Retrieve the poll message
    poll_message = await interaction.original_response()

    # Add voting reactions
    await poll_message.add_reaction("👍")
    await poll_message.add_reaction("👎")


# ==========================================
# START BOT
# ==========================================

bot.run(
    token,
    log_handler=handler,
    log_level=logging.INFO
)