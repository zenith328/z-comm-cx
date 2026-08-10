export function maskName(name: string | null | undefined): string {
  if (!name) return '-'
  if (name.length <= 1) return name
  if (name.length === 2) return `${name[0]}*`
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

export function maskPhone(phone: string | null | undefined): string {
  if (!phone) return '-'
  const chars = phone.split('')
  const digitIndexes: number[] = []
  chars.forEach((c, i) => {
    if (/\d/.test(c)) digitIndexes.push(i)
  })
  const total = digitIndexes.length
  if (total <= 7) return phone

  const maskStart = 3
  const maskEnd = total - 4
  digitIndexes.forEach((charIndex, digitPos) => {
    if (digitPos >= maskStart && digitPos < maskEnd) {
      chars[charIndex] = '*'
    }
  })
  return chars.join('')
}
