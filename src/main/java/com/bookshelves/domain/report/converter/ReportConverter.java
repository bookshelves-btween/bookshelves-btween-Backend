package com.bookshelves.domain.report.converter;

import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.domain.report.entity.Report;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportConverter {

  public static Report toReport(ChatRoom chatRoom, Member reporter) {
    return Report.builder().chatRoom(chatRoom).reporterMember(reporter).build();
  }

  public static ReportCreateResponse toReportCreateResponse(Report report) {
    return new ReportCreateResponse(
        report.getId(), report.getChatRoom().getId(), report.getStatus(), report.getCreatedAt());
  }
}
