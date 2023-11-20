# EconomistBot
## Description
EconomistBot is a Java-based Telegram bot designed to provide users with the latest news updates. Leveraging the capabilities of Telegram's bot API and web scraping technologies, it dynamically fetches and delivers news content from reputable sources. This bot is particularly tailored to provide updates from "The Economist," making it a valuable tool for users interested in staying informed about global events and economic news.

## Try the Bot
You can start using the EconomistBot by following this link to the bot on Telegram: [The World in Brief Bot](https://t.me/Theworldinbrief_bot).

## Features
- Real-time news updates directly in Telegram.
- Integration with news sources for up-to-date information.
- Scheduled daily news updates to keep users informed.
- Custom commands for on-demand news retrieval.
- Efficient HTML content parsing and formatting for Telegram.
- Containerization with Docker for consistent deployment environments.
- Continuous deployment and hosting with Railway for seamless updates and scalability.


## Libraries and Technologies

### Java
- **Version**: JDK 11 or higher.
- **Purpose**: Primary programming language for developing the bot.

### JSoup
- **Website**: [JSoup.org](https://jsoup.org/)
- **Purpose**: Used for parsing HTML and scraping news content from web pages.

### Telegram Bot API
- **Documentation**: [Telegram Bot API](https://core.telegram.org/bots/api)
- **Purpose**: Provides the necessary tools and functionalities to interact with users on Telegram.

### Scheduled Executor Service
- **JavaDoc**: [ScheduledExecutorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html)
- **Purpose**: Used for scheduling daily news updates at specific times.

### Eclipse Jetty
- **Website**: [Eclipse Jetty](https://www.eclipse.org/jetty/)
- **Purpose**: Provides a web server and `javax.servlet` container for handling HTTP requests and serving health check endpoints.

### Logging
- **Technology**: Java Util Logging
- **Purpose**: For logging information, warnings, and errors during the application's runtime.

### Gradle
- **Purpose**: Dependency management and project build tool.

### Environment Configuration
- **Purpose**: Managing configuration properties such as Telegram bot token and username.


## Configuration
Configure your API key in the config.properties:
```shell
bot.token=YOUR_BOT_TOKER_HERE
bot.username=YOUR_BOT_USERMANE_HERE
```

## Email
rpilyushin [at] gmail.com
