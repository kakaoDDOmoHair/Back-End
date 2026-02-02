임금명세서 PDF(이메일 첨부) 한글 표시를 위한 폰트 폴더입니다.

다음 중 하나의 파일을 이 폴더에 넣어 주세요.

1) malgun.ttf (맑은 고딕)
   - Windows: C:\Windows\Fonts\malgun.ttf 를 복사해 이 폴더에 붙여넣기
   - Docker/Linux 배포 시 위 파일을 넣어두면 서버에서도 한글이 정상 표시됩니다.

2) NotoSansKR-Regular.ttf (무료, 재배포 가능)
   - https://fonts.google.com/noto/specimen/Noto+Sans+KR 에서 다운로드 후
     Noto Sans KR 폰트 패키지에서 Regular.ttf 를 이 폴더에 넣고
     파일명을 NotoSansKR-Regular.ttf 로 저장

파일을 넣은 뒤 백엔드를 다시 빌드·배포하면 이메일로 받는 PDF에서도 틀과 한글이 보입니다.
