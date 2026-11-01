package com.kuliahku.di

import com.kuliahku.notification.parser.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @IntoSet
    abstract fun bindMyBcaParser(parser: MyBcaParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindMyBcaAlternativeParser(parser: MyBcaAlternativeParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindBcaParser(parser: BcaParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindBcaLegacyParser(parser: BcaLegacyParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindBcaDigitalParser(parser: BcaDigitalParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindBrimoParser(parser: BrimoParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindLivinParser(parser: LivinParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindDanaParser(parser: DanaParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindGopayParser(parser: GopayParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindOvoParser(parser: OvoParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindShopeeParser(parser: ShopeeParser): NotificationParser
}