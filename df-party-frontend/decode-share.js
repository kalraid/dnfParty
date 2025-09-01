const shareData = 'eJyllE1rFEEQhv9Ln1foj6r%2B2LM%2FwHvIobq7GoPrB0a8hByUFULiTSJGWAkoxIOBVRLIIb9oZ%2FIfrBGjQiYi7mXoLqafrnnft2ZHVTVVjyjz7M59elrVRJGabqh7d%2FuD49XFsvv4WUqrb8vV%2BWm%2FOFt9XfaHl1Lp5nur87Pu5DMNm1cX3ZuFPPv9T1f7F93xniyG%2Bum8W1x2H05l3R8erZbv%2B8Xr%2FuWX%2FvhFdzBwr97OhyPzZXey1x8ddgfv1OZEPZEGNnbU1tAZJx%2BYOWEICcClGL2zwQRdPTUTmkC25bVMD2g29F6fy270rpmaNppt8%2B7kJ1pnFlbyCDZira2hrlCwuKQh1ziOvu1Tb9ChFPahGOMiQLCyK4S2JYtSxFxH6SN6jKCLTpqKtAsGSCNbH6LV1TZASiXm8cb%2FNOwXcnNyLbTD4jHo4kET6ZqjTi1aNiWW5ptZR2hXNMVKtuWUodSakZ0XY5mLZqhlDSmaplBrBSgJWsoJIppMDpzROVgT1%2FUQvGY0GorLnKrRKRauScDsQqNR%2Bs1ZGVG7RKroM6Pkr3FuwNaE7JGAtTMO1%2Bs71VZDkySQhISzXBaMDEvOvmFgewt9NCDXSKtdyJSbjVUcdGAkch5dRUAZTDsuxd89%2FK2GLQMcDQJw9CFZJs2%2BotMmtkTjLv7jkLsGJrmqLYl9oUWJss%2FQrEXxkXiN7BGzScPPI5aUWiawhmXWqRjHUSbp%2FzzclB%2FgYzXdUQ9%2FnN3eKmp3op6pqQnoAzgfbLB69zvqoCea';

// URL 디코딩
const decodedShareData = decodeURIComponent(shareData);
console.log('URL 디코딩 완료, 길이:', decodedShareData.length);

// Base64 디코딩
const compressed = atob(decodedShareData);
console.log('Base64 디코딩 완료, 압축 데이터 크기:', compressed.length);

// Uint8Array로 변환
const compressedArray = new Uint8Array(compressed.length);
for (let i = 0; i < compressed.length; i++) {
  compressedArray[i] = compressed.charCodeAt(i);
}

console.log('Uint8Array 변환 완료');

// Node.js의 zlib 사용
try {
  const zlib = require('zlib');
  const jsonString = zlib.inflateSync(compressedArray);
  const partyData = JSON.parse(jsonString);
  
  console.log('=== 파티 데이터 복호화 완료 ===');
  console.log('던전:', partyData.d);
  console.log('모험단 목록:', partyData.a);
  console.log('파티 수:', partyData.p ? partyData.p.length : 0);
  console.log('최적화 모드:', partyData.o?.m);
  console.log('타임스탬프:', partyData.t);
  
  if (partyData.p) {
    console.log('\n=== 파티 구성 상세 ===');
    partyData.p.forEach((party, index) => {
      console.log(`\n파티 ${index + 1}:`);
      party.forEach((slot, slotIndex) => {
        if (slot) {
          console.log(`  슬롯 ${slotIndex + 1}: ID=${slot.id}, 서버=${slot.s}, 모험단=${slot.adv}, 잠금=${slot.l}`);
        }
      });
    });
  }
  
} catch (error) {
  console.error('압축 해제 실패:', error.message);
  console.log('압축된 데이터 (처음 100바이트):', compressedArray.slice(0, 100));
}
