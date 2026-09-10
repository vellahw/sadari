package org.our.sadari.readingClub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.alim.service.AlimService;
import org.our.sadari.book.mapper.BookMapper;
import org.our.sadari.global.common.code.util.CodeUtil;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.global.common.util.MessageUtils;
import org.our.sadari.readingClub.dto.ReadingClubDto;
import org.our.sadari.readingClub.mapper.ReadingClubMapper;
import org.our.sadari.readingClub.mapper.ReadingClubMembershipMapper;
import org.our.sadari.report.mapper.ReportMapper;
import org.our.sadari.social.service.UserBlockService;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * fileName       : ReadingClubAccessTest
 * author         : SeungHyeon.Kang
 * date           : 2026-08-24
 * description    : 비공개 독서 모임 상세의 활성 회원 접근 경계를 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-24        SeungHyeon.Kang    최초 생성
 * 2026-09-01        HanWon.Jang        자진 탈퇴 Mapper 의존성 반영
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 */
@ExtendWith(MockitoExtension.class)
class ReadingClubAccessTest {

    // 독서 모임 데이터 접근 객체
    @Mock
    private ReadingClubMapper readingClubMapper;
    // 모임 자진 탈퇴 데이터 정리 객체
    @Mock
    private ReadingClubMembershipMapper readingClubMembershipMapper;
    // 사용자 입력 비속어 검사 서비스
    @Mock
    private BadWordDetectionService badWordDetectionService;
    // 사용자 알림 발송 서비스
    @Mock
    private AlimService alimService;
    // 도서 마스터 데이터 접근 객체
    @Mock
    private BookMapper bookMapper;
    // 독후감 데이터 접근 객체
    @Mock
    private ReportMapper reportMapper;

    @Mock
    private UserBlockService userBlockService;
    // 공통코드 조회 도구
    @Mock
    private CodeUtil codeUtil;
    // 채팅 화면 열람 여부 조회 서비스
    @Mock
    private ClubChatViewService clubChatViewService;
    // 독서 모임 접근 정책 테스트 대상
    private ReadingClubServiceImpl readingClubService;

    /** 각 테스트에 독립된 독서 모임 서비스를 생성함 */
    @BeforeEach
    void setUp() {
        // 실패 응답에서 사용할 서버 공통 메시지 소스를 생성함
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // 서버 공통 메시지 프로퍼티를 테스트 조회 기준으로 설정함
        messageSource.setBasename("messages");
        // 한글 메시지 원문이 손상되지 않도록 인코딩을 설정함
        messageSource.setDefaultEncoding("UTF-8");
        // ResultData 실패 응답이 공통 메시지 소스를 사용하도록 초기화함
        new MessageUtils().setMessageSource(messageSource);

        // 독서 모임 서비스 단위 테스트 대상을 생성함
        readingClubService = new ReadingClubServiceImpl(
                readingClubMapper, readingClubMembershipMapper, badWordDetectionService
              , alimService, bookMapper, reportMapper
              , userBlockService, codeUtil, clubChatViewService);
    }

    /** 초대 관계가 활성 회원 조회에서 제외되면 비공개 상세 접근을 거절함 */
    @Test
    void privateClubRejectsInvite() {
        // SQL에서 초대 관계가 제외된 비공개 모임 조회 결과를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubNumb(10L);
        club.setClubVisb("PRIVATE");
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(club);

        // 초대 상태 사용자가 비공개 모임 상세를 조회함
        ResultData result = readingClubService.getClubDtl(20L, 10L);

        // 접근 거절과 관계 데이터 미조회를 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getClubCategoryList(10L);
    }

    /** 활성 회원은 비공개 모임 상세를 정상 조회함 */
    @Test
    void privateClubAllowsActive() {
        // 활성 회원 관계를 가진 비공개 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubNumb(10L);
        club.setClubVisb("PRIVATE");
        club.setMembStat("ACTIVE");
        club.setJoinType("OPEN");
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(club);
        when(readingClubMapper.getClubCategoryList(10L)).thenReturn(List.of());

        // 활성 회원이 비공개 모임 상세를 조회함
        ResultData result = readingClubService.getClubDtl(20L, 10L);

        // 정상 응답과 관계 데이터 조회를 검증함
        assertEquals(200, result.getCode());
        assertEquals(club, result.getData());
        verify(readingClubMapper).getClubCategoryList(10L);
    }

    /** 상세 SQL이 초대 관계를 활성 회원으로 취급하지 않는지 검증함 */
    @Test
    void mapperFiltersInviteState() throws IOException {
        // 빌드 산출물에 포함된 독서 모임 Mapper XML을 엶
        String resourcePath = "org/our/sadari/readingClub/mapper/ReadingClubMapper.xml";
        InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(resource);

        // 줄바꿈과 들여쓰기를 제거해 활성 회원 Join 조건을 안정적으로 비교함
        String mapperXml;
        try (resource) {
            mapperXml = new String(resource.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        }

        // 로그인 사용자의 관계 Join이 활성 회원 상태로 제한되는지 검증함
        assertTrue(mapperXml.contains("LEFT JOIN TB_CLMEMX MINE "
                + "ON MINE.CLUB_NUMB = CLUB.CLUB_NUMB "
                + "AND MINE.USER_NUMB = #{userNumb} "
                + "AND MINE.MEMB_STAT = #{memberActive}"));
    }
}
