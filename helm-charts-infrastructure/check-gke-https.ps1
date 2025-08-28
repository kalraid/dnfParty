# =============================
# GKE HTTPS 점검 스크립트
# =============================

# 수정 필요
$namespace = "dfo"
$ingressName = "dnf"
$certName = "dnfpartyhelper"
$domainList = @("dnfpartyhelper.xyz","www.dnfpartyhelper.xyz")

Write-Host "=== 1️⃣ DNS 확인 ==="
foreach ($domain in $domainList) {
    Write-Host "`n확인: $domain"
    nslookup $domain
}

Write-Host "`n=== 2️⃣ Ingress annotation 확인 ==="
kubectl get ingress $ingressName -n $namespace -o yaml | Select-String "managed-certificates"
kubectl get ingress $ingressName -n $namespace -o yaml | Select-String "ingress.class"

Write-Host "`n=== 3️⃣ ManagedCertificate 상태 확인 ==="
kubectl describe managedcertificate $certName -n $namespace | Select-String "Certificate Status|Domain"

Write-Host "`n=== 4️⃣ GCP Forwarding Rule / Target Proxy / URL Map 확인 ==="
$httpsForwardingRule = kubectl describe ingress $ingressName -n $namespace | Select-String "https-forwarding-rule"
Write-Host "https-forwarding-rule: $httpsForwardingRule"

$targetProxy = kubectl describe ingress $ingressName -n $namespace | Select-String "https-target-proxy"
Write-Host "Target Proxy: $targetProxy"

$urlMap = kubectl describe ingress $ingressName -n $namespace | Select-String "url-map"
Write-Host "URL Map: $urlMap"

Write-Host "`n=== 5️⃣ Firewall 443 확인 ==="
gcloud compute firewall-rules list --filter="NETWORK:default" | Select-String "ALLOW.*443"

Write-Host "`n=== 6️⃣ HTTPS 통신 테스트 ==="
foreach ($domain in $domainList) {
    Write-Host "`n테스트: https://$domain/"
    curl.exe -vk https://$domain/
}

Write-Host "`n=== 점검 완료 ==="