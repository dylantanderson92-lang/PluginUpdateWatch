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


@bot.command()
@commands.has_permissions(administrator=True)
async def announce(ctx, channel: discord.TextChannel, title: str, *, message: str):
    """
    Usage: !announce #channel "Your Title Here" Your announcement message here
    """
    embed = discord.Embed(
        title=f"📢 {title}",  # Uses the title you provided in the command
        description=message,
        color=discord.Color.blue()
    )
    embed.set_footer(text=f"Announced by {ctx.author.display_name}")

    await channel.send(embed=embed)
    await ctx.send(f"Announcement successfully sent to {channel.mention}!", delete_after=5)


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