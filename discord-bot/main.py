import app_commands
import discord
from discord.ext import commands
import logging
from dotenv import load_dotenv
import os

load_dotenv()
token = os.getenv("DISCORD_TOKEN")

handler = logging.FileHandler(filename='discord.log', encoding='utf-8', mode='w')
intents = discord.Intents.default()
intents.message_content = True
intents.members = True

bot = commands.Bot(command_prefix='!', intents=intents)

@bot.event
async def on_ready():
    print(f"We are ready to go in, {bot.user.name}")

@bot.event
async def on_member_join(member):
    await member.send(f"Welcome to the PluginUpdateWatch server {member.name}")


@bot.tree.command(name='announce', description='Send a formatted announcement in a channel')
@app_commands.default_permissions(manage_guild=True)
@app_commands.describe(channel='Channel for announcement', title='Announcement title',
                       message='Announcement body')
async def announce(interaction: discord.Interaction, channel: discord.TextChannel,
                   title: str, message: str):
    if not interaction.guild or not isinstance(interaction.user, discord.Member) or not admin(interaction.user):
        return await interaction.response.send_message('You need Manage Server permission.', ephemeral=True)
    if channel.guild.id != interaction.guild.id:
        return await interaction.response.send_message('Select a channel in this server.', ephemeral=True)
    if len(title) > 256 or len(message) > 4000:
        return await interaction.response.send_message('Title or message is too long.', ephemeral=True)
    embed = discord.Embed(title=f'📢 {title}', description=message, color=0x5865F2)
    embed.set_footer(text=f'Posted by {interaction.set.server_name}')
    try:
        await channel.send(embed=embed, allowed_mentions=discord.AllowedMentions.none())
    except discord.HTTPException:
        return await interaction.response.send_message('Cannot post there. Check my channel permissions.', ephemeral=True)
    await interaction.response.send_message(f'Announcement posted in {channel.mention}.', ephemeral=True)


# Error handling if someone without permissions tries to use it
@announce.error
async def announce_error(ctx, error):
    if isinstance(error, commands.MissingPermissions):
        await ctx.send("You do not have permission to use this command.", delete_after=5)

@bot.command()
async def dm(ctx, *, msg):
    await ctx.author.send(f"You said {msg}")

@bot.command()
async def reply(ctx):
    await ctx.reply("This is a reply to your message!")

@bot.command()
async def poll(ctx, *, question):
    embed = discord.Embed(title="New Poll", description=question)
    poll_message = await ctx.send(embed=embed)
    await poll_message.add_reaction("👍")
    await poll_message.add_reaction("👎")

bot.run(token, log_handler=handler, log_level=logging.DEBUG)