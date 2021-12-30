package kr.flab.fream.mybatis.mapper.auction

import kr.flab.domain.auction.AuctionFixtures
import kr.flab.fream.DatabaseClearConfig
import kr.flab.fream.DatabaseTest
import kr.flab.fream.controller.auction.AuctionSummaryByPriceAndSizeWithQuantity
import kr.flab.fream.domain.auction.AuctionSearchOption
import kr.flab.fream.domain.auction.model.AuctionType
import kr.flab.fream.domain.user.model.User
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

import static kr.flab.domain.product.ProductFixtures.nikeDunkLowRetroBlack
import static org.assertj.core.api.Assertions.assertThat

@MybatisTest
@Import(DatabaseClearConfig)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuctionMapperSpec extends DatabaseTest {

    @Autowired
    AuctionMapper auctionMapper

    def "save Auction"() {
        given:
        def product = getNikeDunkLowRetroBlack()
        def auction = AuctionFixtures.create("284000", product, product.getSize(1L), 60, AuctionType.ASK)

        expect:
        auctionMapper.create(auction) == 1
    }

    def "cancel auction by removing that"() {
        given:
        def product = getNikeDunkLowRetroBlack()
        def auction = AuctionFixtures.create("284000", product, product.getSize(1L), 60, type)

        auctionMapper.create(auction)

        expect:
        auctionMapper.cancel(auction) == 1

        and:
        def updatedAuction = auctionMapper.getAuction(auction.id)
        updatedAuction.canceledAt != null

        where:
        type << [AuctionType.ASK, AuctionType.BID]
    }

    def "update Bid"() {
        given:
        def product = getNikeDunkLowRetroBlack()
        def auction = AuctionFixtures.create("284000", product, product.getSize(1L), 60, AuctionType.BID)
        auctionMapper.create(auction)

        auction.update(new BigDecimal("280000"), 30L)

        expect:
        auctionMapper.update(auction) == 1
    }

    def "set bidder"() {
        given:
        def product = getNikeDunkLowRetroBlack()
        def auction = AuctionFixtures.create("284000", product, product.getSize(1L), 60, AuctionType.BID)

        auctionMapper.create(auction)

        def user = new User()
        user.id = 1L

        auction.sign(user)

        expect:
        auctionMapper.update(auction) == 1
        def resultAuction = auctionMapper.getAuction(auction.getId())
        def bidder = resultAuction.getBidder()
        bidder != null
        bidder.getId() == 1L
    }

    def "get auction using x lock"() {
        given:
        def product = getNikeDunkLowRetroBlack()
        def auction = AuctionFixtures.create("284000", product, product.getSize(1L), 60, AuctionType.BID)

        auctionMapper.create(auction)

        when:
        def auctionWithXLock = auctionMapper.getAuctionForUpdate(auction.getId())

        then:
        auctionWithXLock.getId() == auction.getId()
    }

    @Sql("/db-test-data/auction/get-ask-summaries.sql")
    def "get ASK summaries"() {
        given:
        def type = AuctionType.ASK
        def productId = 2L

        expect:
        def summaries = auctionMapper.getAuctionSummaries(type, productId, null, null)
        assertThat(summaries)
            .usingRecursiveComparison()
            .isEqualTo(Arrays.asList(
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("550000.00"), 2, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("575000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("576000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("577000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("578000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("579000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("275", new BigDecimal("583000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("280", new BigDecimal("583000.00"), 2, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("280", new BigDecimal("584000.00"), 1, AuctionType.ASK),
                new AuctionSummaryByPriceAndSizeWithQuantity("280", new BigDecimal("588000.00"), 1, AuctionType.ASK),
            ))
    }

    @Sql("/db-test-data/auction/get-bid-summaries.sql")
    def "get BID summaries"() {
        given:
        def type = AuctionType.BID
        def productId = 2L
        def sizeId = 10L

        when:
        def summaries =
            auctionMapper.getAuctionSummaries(type, productId, sizeId, lastPrice)

        then:
        assertThat(summaries)
            .usingRecursiveComparison()
            .isEqualTo(expect)

        where:
        lastPrice << [null, new BigDecimal("507000")]
        expect << [
            Arrays.asList(
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("530000.00"), 3, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("529000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("528000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("525000.00"), 2, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("524000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("522000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("518000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("515000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("512000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("507000.00"), 1, AuctionType.BID),
            ),
            Arrays.asList(
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("500000.00"), 6, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("499000.00"), 1, AuctionType.BID),
                new AuctionSummaryByPriceAndSizeWithQuantity("265", new BigDecimal("495000.00"), 1, AuctionType.BID),
            ),
        ]
    }

    @Sql("/db-test-data/auction/get-ask-summaries.sql")
    def "get lowest ASK auction"() {
        given:
        def type = AuctionType.ASK
        def productId = 2L

        def searchOption = AuctionSearchOption.builder()
            .auctionType(type)
            .items(1)
            .productId(productId)
            .build()

        when:
        def auctions =
            auctionMapper.getAuctions(searchOption)

        then:
        auctions.size() == 1

        and:
        assertThat(auctions.get(0).getPrice()).isEqualTo(new BigDecimal("550000.00"))
    }

    @Sql("/db-test-data/auction/get-bid-summaries.sql")
    def "get highest BID"() {
        given:
        def type = AuctionType.BID
        def productId = 2L
        def sizeId = 10L

        def searchOption = AuctionSearchOption.builder()
            .auctionType(type)
            .items(1)
            .productId(productId)
            .sizeId(sizeId)
            .build()

        when:
        def auctions =
            auctionMapper.getAuctions(searchOption)

        then:
        auctions.size() == 1

        and:
        assertThat(auctions.get(0).getPrice()).isEqualTo(new BigDecimal("530000.00"))
    }

}
